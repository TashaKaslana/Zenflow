package org.phong.zenflow.plugin.subdomain.schema.registry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized registry for schema locations.
 * This component extracts the schema index from PluginNodeSynchronizer
 * to break the circular dependency between components.
 */
@Component
@Slf4j
public class SchemaIndexRegistry {

    /**
     * Schema index for fast lookups by UUID
     */
    @Getter
    private final ConcurrentHashMap<String, SchemaLocation> schemaIndex = new ConcurrentHashMap<>();

    /**
     * Register a schema location in the index
     * @param nodeId The node identifier
     * @param location The schema location information
     * @return true if added successfully
     */
    public boolean addSchemaLocation(String nodeId, SchemaLocation location) {
        if (nodeId != null && location != null) {
            schemaIndex.put(nodeId, location);
            log.debug("Indexed schema location for node ID {}: {}", nodeId, location.clazz().getName());
            return true;
        }
        return false;
    }

    /**
     * Get a schema location by node identifier
     * @param nodeId The node identifier
     * @return The schema location or null if not found
     */
    public SchemaLocation getSchemaLocation(String nodeId) {
        return schemaIndex.get(nodeId);
    }

    /**
     * Check if a schema location exists for the given node id
     * @param nodeId The node identifier
     * @return true if a schema location exists
     */
    public boolean hasSchemaLocation(String nodeId) {
        return schemaIndex.containsKey(nodeId);
    }

    /**
     * Get the number of schema locations in the index
     * @return The size of the schema index
     */
    public int getSchemaIndexSize() {
        return schemaIndex.size();
    }

    /**
     * Schema location data holder
     */
    public record SchemaLocation(Class<?> clazz, String schemaPath) {
        public SchemaLocation {
            if (clazz == null) {
                throw new IllegalArgumentException("Class cannot be null");
            }
            if (schemaPath == null) {
                schemaPath = "";
            }
        }

        public boolean hasCustomPath() {
            return !schemaPath.isEmpty();
        }
    }
}
