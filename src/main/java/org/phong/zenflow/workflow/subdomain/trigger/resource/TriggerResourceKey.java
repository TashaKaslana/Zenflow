package org.phong.zenflow.workflow.subdomain.trigger.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Resource key for trigger resource pooling, following the same pattern as DbConnectionKey.
 * This enables efficient resource sharing and caching similar to GlobalDbConnectionPool.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode
public class TriggerResourceKey {

    /**
     * The primary resource identifier (e.g., Discord bot token, WebSocket URL)
     */
    private final String resourceIdentifier;

    /**
     * Additional configuration that affects resource creation
     * (e.g., connection settings, authentication details)
     */
    private final Map<String, Object> resourceConfig;

    /**
     * Simple constructor for basic resource keys
     */
    public TriggerResourceKey(String resourceIdentifier) {
        this(resourceIdentifier, Map.of());
    }
}
