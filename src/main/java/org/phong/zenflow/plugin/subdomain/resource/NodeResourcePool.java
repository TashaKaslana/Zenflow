package org.phong.zenflow.plugin.subdomain.resource;

import java.util.UUID;

/**
 * Generic resource pool for node or trigger executors that need shared resources.
 * Provides methods for acquiring, tracking, and cleaning up shared clients.
 *
 * @param <T> The type of resource being managed
 * @param <C> Configuration type required to create the resource
 */
public interface NodeResourcePool<T, C> {

    /**
     * Get or create a resource for the given key.
     *
     * @param resourceKey unique identifier for the resource
     * @param config configuration needed to create the resource
     * @return the shared resource instance
     */
    T getOrCreateResource(String resourceKey, C config);

    /**
     * Register a node as using this resource. Enables reference counting
     * for cleanup.
     */
    void registerNodeUsage(String resourceKey, UUID nodeId);

    /**
     * Unregister a node from using this resource. Will cleanup the resource
     * if no more nodes are using it.
     */
    void unregisterNodeUsage(String resourceKey, UUID nodeId);

    /**
     * Check if a resource exists and is considered healthy.
     */
    boolean isResourceHealthy(String resourceKey);

    /**
     * Force cleanup of unused resources (for maintenance).
     */
    void cleanupUnusedResources();

    /**
     * Acquire a resource for the given key and automatically unregister
     * usage when the returned handle is closed.
     *
     * @param resourceKey unique identifier for the resource
     * @param nodeId the node acquiring the resource
     * @param config configuration needed to create the resource
     * @return handle wrapping the shared resource
     */
    default ScopedNodeResource<T> acquire(String resourceKey, UUID nodeId, C config) {
        T resource = getOrCreateResource(resourceKey, config);
        registerNodeUsage(resourceKey, nodeId);
        return new ScopedNodeResource<>(this, resourceKey, nodeId, resource);
    }
}
