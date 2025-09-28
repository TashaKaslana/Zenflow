package org.phong.zenflow.plugin.subdomain.resource;

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
    void registerNodeUsage(String resourceKey);

    /**
     * Unregister a node from using this resource. Will cleanup the resource
     * if no more nodes are using it.
     */
    void unregisterNodeUsage(String resourceKey);

    /**
     * Check if a resource exists and is considered healthy.
     */
    boolean isResourceHealthy(String resourceKey);

    ScopedNodeResource<T> acquire(String resourceKey, C config);
}
