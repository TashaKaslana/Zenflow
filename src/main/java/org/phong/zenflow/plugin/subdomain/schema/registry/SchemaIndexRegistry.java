package org.phong.zenflow.plugin.subdomain.schema.registry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized registry for schema locations, supporting both plugin-level and node-level schemas.
 * This component is populated by synchronizing at startup to create an in-memory index
 * of schemas, which allows for fast, file-based lookups that avoid database access.
 */
@Component
@Slf4j
public class SchemaIndexRegistry {

    /**
     * In-memory index of schema locations, keyed by a unique identifier (e.g., plugin UUID or node UUID).
     */
    @Getter
    private final ConcurrentHashMap<String, SchemaLocation> schemaIndex = new ConcurrentHashMap<>();

    /**
     * Registers a schema location in the index.
     *
     * @param key      The unique identifier for the schema (e.g., a plugin ID or node ID).
     * @param location The location of the schema definition.
     * @return true if the location was added successfully, false otherwise.
     */
    public boolean addSchemaLocation(String key, SchemaLocation location) {
        if (key != null && location != null) {
            schemaIndex.put(key, location);
            log.debug("Indexed schema location for key {}: {}", key, location.clazz().getName());
            return true;
        }
        return false;
    }

    /**
     * Retrieves a schema location by its unique identifier.
     *
     * @param key The unique identifier (e.g., plugin ID or node ID).
     * @return The schema location, or null if not found.
     */
    public SchemaLocation getSchemaLocation(String key) {
        return schemaIndex.get(key);
    }

    /**
     * Checks if a schema location exists for the given identifier.
     *
     * @param key The unique identifier (e.g., plugin ID or node ID).
     * @return true if a schema location exists, false otherwise.
     */
    public boolean hasSchemaLocation(String key) {
        return schemaIndex.containsKey(key);
    }

    /**
     * Gets the total number of schema locations currently in the index.
     *
     * @return The size of the schema index.
     */
    public int getSchemaIndexSize() {
        return schemaIndex.size();
    }

    /**
     * Holds the location of a schema definition, typically the class it's associated with
     * and the path to the schema file.
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

        /**
         * Checks if a custom schema path is defined.
         * @return true if the path is not empty.
         */
        public boolean hasCustomPath() {
            return !schemaPath.isEmpty();
        }
    }
}