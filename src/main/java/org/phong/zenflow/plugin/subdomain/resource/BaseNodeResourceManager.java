package org.phong.zenflow.plugin.subdomain.resource;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base implementation of {@link NodeResourcePool} using Caffeine cache and
 * reference counting.
 *
 * @param <T> resource type
 * @param <C> configuration type
 */
@Slf4j
public abstract class BaseNodeResourceManager<T, C> implements NodeResourcePool<T, C> {
    private final Duration idleEviction = Duration.ofMinutes(10);

    private final Cache<@NonNull String, Tracked<T>> resourceCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfter(new Expiry<@NonNull String, @NonNull Tracked<T>>() {
                public long expireAfterCreate(String k, Tracked<T> v, long now) {
                    return v.refs.get() > 0 ? Long.MAX_VALUE : idleEviction.toNanos();
                }

                public long expireAfterUpdate(String k, Tracked<T> v, long now, long cur) {
                    return v.refs.get() > 0 ? Long.MAX_VALUE : idleEviction.toNanos();
                }

                public long expireAfterRead(String k, Tracked<T> v, long now, long cur) {
                    return v.refs.get() > 0 ? Long.MAX_VALUE : idleEviction.toNanos();
                }
            })
            .removalListener((String k, Tracked<T> v, RemovalCause c) -> onResourceRemoval(k, v.resource, c))
            .build();

    @Override
    public T getOrCreateResource(String resourceKey, C config) {
        Tracked<T> trackedResource = getOrCreateTrackedResource(resourceKey, config);

        return trackedResource.resource;
    }

    private Tracked<T> getOrCreateTrackedResource(String resourceKey, C config) {
        return resourceCache.get(resourceKey, k -> {
            log.info("Creating new resource for key: {}", k);
            return new Tracked<>(createResource(k, config));
        });
    }

    /**
     * Acquire a resource for the given key and automatically unregister
     * usage when the returned handle is closed.
     *
     * @param resourceKey unique identifier for the resource
     * @param nodeId      the node acquiring the resource
     * @param config      configuration needed to create the resource
     * @return handle wrapping the shared resource
     */
    @Override
    public ScopedNodeResource<T> acquire(String resourceKey, UUID nodeId, C config) {
        Tracked<T> trackedResource = getOrCreateTrackedResource(resourceKey, config);
        trackedResource.refs.incrementAndGet();
        return new ScopedNodeResource<>(this, resourceKey, nodeId, trackedResource.resource);
    }

    @Override
    public void registerNodeUsage(String resourceKey) {
        Tracked<T> tracked = resourceCache.getIfPresent(resourceKey);
        if (tracked == null) {
            return;
        }

        int referenceCount = tracked.refs.incrementAndGet();
        log.debug("Registered a node for resource {} with current usage count: {}", resourceKey, referenceCount);
    }

    @Override
    public void unregisterNodeUsage(String resourceKey) {
        var tracked = resourceCache.getIfPresent(resourceKey);
        if (tracked == null) return;

        tracked.refs.decrementAndGet();
    }

    @Override
    public boolean isResourceHealthy(String resourceKey) {
        T resource = getExistingResource(resourceKey);
        return resource != null && checkResourceHealth(resource);
    }

    /**
     * Snapshot of current resource usage: resource key to active node count.
     *
     * @return immutable copy of usage counts
     */
    protected Map<String, Integer> getUsageSnapshot() {
        Map<String, Integer> snapshot = new HashMap<>();
        resourceCache.asMap()
                .forEach((key, tracked) -> snapshot.put(key, tracked.refs.get()));
        return snapshot;
    }

    /**
     * Get an existing resource if present in the cache.
     * Subclasses can use this for health checks or additional logic.
     */
    protected T getExistingResource(String resourceKey) {
        Tracked<T> tracked = resourceCache.getIfPresent(resourceKey);
        return tracked != null ? tracked.resource : null;
    }

    /**
     * Subclasses can override to implement health checking of resources.
     */
    protected boolean checkResourceHealth(T resource) {
        return true;
    }

    private void onResourceRemoval(String key, T resource, RemovalCause cause) {
        if (resource != null) {
            log.info("Cleaning up resource {} due to {}", key, cause);
            try {
                cleanupResource(resource);
            } catch (Exception e) {
                log.error("Error cleaning up resource {}: {}", key, e.getMessage(), e);
            }
        }
    }


    protected abstract T createResource(String resourceKey, C config);

    protected abstract void cleanupResource(T resource);
}
