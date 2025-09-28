package org.phong.zenflow.workflow.subdomain.trigger.resource;

import org.phong.zenflow.workflow.subdomain.trigger.dto.TriggerContext;

import java.util.Map;

/**
 * Configuration interface for trigger resources, following the same pattern as ResolvedDbConfig.
 * This is generic and flexible to support any trigger type.
 */
public interface TriggerResourceConfig {

    /**
     * Get the unique identifier for this resource.
     * Similar to how DbConnectionKey works for database connections.
     * For Discord: the bot token
     * For WebSocket: the connection URL
     * For Database: connection string
     */
    String getResourceIdentifier();

    /**
     * Get the raw configuration map from WorkflowTrigger.config
     */
    Map<String, Object> getContextMap();

    /**
     * Get a specific configuration value
     */
    default <T> T getConfigValue(String key, Class<T> type) {
        Object value = getContextMap().get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }
        return null;
    }

    /**
     * Get configuration value with default
     */
    default <T> T getConfigValue(String key, Class<T> type, T defaultValue) {
        T value = getConfigValue(key, type);
        return value != null ? value : defaultValue;
    }

    /**
     * Convert to a resource key for pooling, similar to DbConnectionKey.
     * This enables the same pooling pattern as GlobalDbConnectionPool.
     */
    default TriggerResourceKey toResourceKey() {
        return new TriggerResourceKey(getResourceIdentifier(), getContextMap());
    }

    /**
     * Create from WorkflowTrigger, similar to ResolvedDbConfig.fromInput()
     */
    static TriggerResourceConfig fromTrigger(TriggerContext triggerCtx, String resourceKeyField) {
        return new DefaultResourceConfig(triggerCtx, resourceKeyField);
    }
}
