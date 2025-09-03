package org.phong.zenflow.workflow.subdomain.trigger.resource;

import java.util.UUID;

/**
 * Generic resource manager for trigger executors that need shared resources.
 * Similar to GlobalDbConnectionPool but for any type of trigger resource.
 *
 * @param <T> The type of resource being managed (e.g., JDA, WebSocketClient, etc.)
 */
public interface TriggerResourceManager<T> {

    /**
     * Get or create a resource for the given key.
     * This follows the same pattern as GlobalDbConnectionPool.getOrCreate()
     *
     * @param resourceKey Unique identifier for the resource (e.g., discord token, webhook URL)
     * @param config Configuration needed to create the resource
     * @return The shared resource instance
     */
    T getOrCreateResource(String resourceKey, TriggerResourceConfig config);

    /**
     * Register a trigger as using this resource.
     * This enables reference counting for cleanup.
     */
    void registerTriggerUsage(String resourceKey, UUID triggerId);

    /**
     * Unregister a trigger from using this resource.
     * Will cleanup the resource if no more triggers are using it.
     */
    void unregisterTriggerUsage(String resourceKey, UUID triggerId);

    /**
     * Check if a resource exists and is healthy
     */
    boolean isResourceHealthy(String resourceKey);

    /**
     * Force cleanup of unused resources (for maintenance)
     */
    void cleanupUnusedResources();

    /**
     * Get the resource key that this trigger should use.
     * This allows the manager to determine resource sharing logic.
     */
    default String generateResourceKey(TriggerResourceConfig config) {
        return config.getResourceIdentifier();
    }
}
