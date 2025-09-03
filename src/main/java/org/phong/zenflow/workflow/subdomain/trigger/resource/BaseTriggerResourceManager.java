package org.phong.zenflow.workflow.subdomain.trigger.resource;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base implementation of TriggerResourceManager following the same pattern as GlobalDbConnectionPool.
 * Uses Caffeine cache for automatic cleanup and reference counting for resource management.
 */
@Slf4j
public abstract class BaseTriggerResourceManager<T> implements TriggerResourceManager<T> {

    // Make cache private like GlobalDbConnectionPool
    private final Cache<String, T> resourceCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener(this::onResourceRemoval)
            .build();

    // Track which triggers are using which resources for reference counting
    private final ConcurrentHashMap<String, Set<UUID>> resourceUsage = new ConcurrentHashMap<>();

    @Override
    public T getOrCreateResource(String resourceKey, TriggerResourceConfig config) {
        return resourceCache.get(resourceKey, k -> {
            log.info("Creating new resource for key: {}", k);
            return createResource(k, config);
        });
    }

    @Override
    public void registerTriggerUsage(String resourceKey, UUID triggerId) {
        resourceUsage.computeIfAbsent(resourceKey, k -> ConcurrentHashMap.newKeySet()).add(triggerId);
        log.debug("Registered trigger {} for resource {}", triggerId, resourceKey);
    }

    @Override
    public void unregisterTriggerUsage(String resourceKey, UUID triggerId) {
        Set<UUID> triggers = resourceUsage.get(resourceKey);
        if (triggers != null) {
            triggers.remove(triggerId);
            if (triggers.isEmpty()) {
                resourceUsage.remove(resourceKey);
                // Remove from cache to trigger cleanup
                resourceCache.invalidate(resourceKey);
                log.info("No more triggers using resource {}, marked for cleanup", resourceKey);
            }
        }
        log.debug("Unregistered trigger {} from resource {}", triggerId, resourceKey);
    }

    @Override
    public boolean isResourceHealthy(String resourceKey) {
        T resource = resourceCache.getIfPresent(resourceKey);
        return resource != null && checkResourceHealth(resource);
    }

    @Override
    public void cleanupUnusedResources() {
        log.info("Starting cleanup of unused resources");
        resourceCache.cleanUp();
    }

    /**
     * Get an existing resource if it exists and is healthy.
     * This is the proper way to access resources from subclasses.
     */
    protected T getExistingResource(String resourceKey) {
        T resource = resourceCache.getIfPresent(resourceKey);
        return resource != null && checkResourceHealth(resource) ? resource : null;
    }

    /**
     * Called when a resource is removed from the cache.
     * This is where actual resource cleanup happens (similar to closing DataSource).
     */
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

    /**
     * Subclasses implement this to create the actual resource.
     * Similar to GlobalDbConnectionPool.createDataSource()
     */
    protected abstract T createResource(String resourceKey, TriggerResourceConfig config);

    /**
     * Subclasses implement this to cleanup the resource when it's no longer needed.
     * Similar to closing a DataSource or JDA connection.
     */
    protected abstract void cleanupResource(T resource);

    /**
     * Subclasses can override this to implement health checking.
     */
    protected boolean checkResourceHealth(T resource) {
        return true; // Default: assume healthy
    }
}
