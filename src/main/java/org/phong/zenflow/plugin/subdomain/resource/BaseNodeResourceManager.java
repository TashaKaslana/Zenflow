package org.phong.zenflow.plugin.subdomain.resource;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Base implementation of {@link NodeResourcePool} using Caffeine cache and
 * reference counting.
 *
 * @param <T> resource type
 * @param <C> configuration type
 */
@Slf4j
public abstract class BaseNodeResourceManager<T, C> implements NodeResourcePool<T, C> {

    private final Cache<String, T> resourceCache = Caffeine.newBuilder()
            .maximumSize(500)
            .removalListener(this::onResourceRemoval)
            .build();

    private final ConcurrentHashMap<String, Set<UUID>> resourceUsage = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> lastAccess = new ConcurrentHashMap<>();
    private final Duration idleEviction = Duration.ofMinutes(10);

    @Override
    public T getOrCreateResource(String resourceKey, C config) {
        T resource = resourceCache.get(resourceKey, k -> {
            log.info("Creating new resource for key: {}", k);
            return createResource(k, config);
        });
        lastAccess.put(resourceKey, System.currentTimeMillis());
        return resource;
    }

    @Override
    public void registerNodeUsage(String resourceKey, UUID nodeId) {
        resourceUsage.computeIfAbsent(resourceKey, k -> ConcurrentHashMap.newKeySet()).add(nodeId);
        log.debug("Registered node {} for resource {}", nodeId, resourceKey);
    }

    @Override
    public void unregisterNodeUsage(String resourceKey, UUID nodeId) {
        Set<UUID> nodes = resourceUsage.get(resourceKey);
        if (nodes != null) {
            nodes.remove(nodeId);
            if (nodes.isEmpty()) {
                resourceUsage.remove(resourceKey);
                resourceCache.invalidate(resourceKey);
                lastAccess.remove(resourceKey);
                log.info("No more nodes using resource {}, marked for cleanup", resourceKey);
            }
        }
        log.debug("Unregistered node {} from resource {}", nodeId, resourceKey);
    }

    @Override
    public boolean isResourceHealthy(String resourceKey) {
        T resource = getExistingResource(resourceKey);
        return resource != null && checkResourceHealth(resource);
    }

    @Override
    public void cleanupUnusedResources() {
        log.info("Starting cleanup of unused node resources");
        long cutoff = System.currentTimeMillis() - idleEviction.toMillis();
        lastAccess.forEach((key, ts) -> {
            if (ts < cutoff && !resourceUsage.containsKey(key)) {
                resourceCache.invalidate(key);
                lastAccess.remove(key);
                log.debug("Evicted idle resource {}", key);
            }
        });
        resourceCache.cleanUp();
    }

    /**
     * Snapshot of current resource usage: resource key to active node count.
     *
     * @return immutable copy of usage counts
     */
    protected Map<String, Integer> getUsageSnapshot() {
        Map<String, Integer> snapshot = new HashMap<>();
        resourceUsage.forEach((key, nodes) -> snapshot.put(key, nodes.size()));
        return snapshot;
    }

    /**
     * Get an existing resource if present in the cache.
     * Subclasses can use this for health checks or additional logic.
     */
    protected T getExistingResource(String resourceKey) {
        return resourceCache.getIfPresent(resourceKey);
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
