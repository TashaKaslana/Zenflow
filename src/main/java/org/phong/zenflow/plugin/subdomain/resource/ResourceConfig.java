package org.phong.zenflow.plugin.subdomain.resource;

import org.phong.zenflow.workflow.subdomain.trigger.resource.TriggerResourceKey;

import java.util.Map;

public interface ResourceConfig {
    String getResourceIdentifier();

    /**
     * Get the raw configuration map from WorkflowTrigger.config
     */
    Map<String, Object> getContextMap();

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

    default boolean isManual() {
        return false;
    }
}
