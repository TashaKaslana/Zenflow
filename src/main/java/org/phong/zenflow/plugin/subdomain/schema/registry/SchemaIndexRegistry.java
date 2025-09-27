package org.phong.zenflow.plugin.subdomain.schema.registry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized registry for schema locations, supporting plugin-level, descriptor-level, and node-level schemas.
 * This component is populated by synchronizing at startup to create an in-memory index
 * of schemas, which allows for fast, file-based lookups that avoid database access.
 */
@Component
@Slf4j
public class SchemaIndexRegistry {

    private static final String BASE_DESCRIPTOR_KEY_FORMAT = "%s:%s:%s";
    private static final String PROFILE_SECTION = "profile";
    private static final String SETTING_SECTION = "setting";

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
     * Register a descriptor-specific schema location for profile descriptors.
     */
    public boolean addDescriptorSchemaLocation(UUID pluginId, String descriptorId, SchemaLocation location) {
        return addProfileSchemaLocation(pluginId, descriptorId, location);
    }

    public boolean addProfileSchemaLocation(UUID pluginId, String descriptorId, SchemaLocation location) {
        if (pluginId == null || descriptorId == null || descriptorId.isBlank()) {
            return false;
        }
        return addSchemaLocation(buildDescriptorKey(pluginId.toString(), PROFILE_SECTION, descriptorId), location);
    }

    public boolean addProfileSchemaLocation(String pluginKey, String descriptorId, SchemaLocation location) {
        if (pluginKey == null || pluginKey.isBlank() || descriptorId == null || descriptorId.isBlank()) {
            return false;
        }
        return addSchemaLocation(buildDescriptorKey(pluginKey, PROFILE_SECTION, descriptorId), location);
    }

    public boolean addSettingSchemaLocation(UUID pluginId, String descriptorId, SchemaLocation location) {
        if (pluginId == null || descriptorId == null || descriptorId.isBlank()) {
            return false;
        }
        return addSchemaLocation(buildDescriptorKey(pluginId.toString(), SETTING_SECTION, descriptorId), location);
    }

    public boolean addSettingSchemaLocation(String pluginKey, String descriptorId, SchemaLocation location) {
        if (pluginKey == null || pluginKey.isBlank() || descriptorId == null || descriptorId.isBlank()) {
            return false;
        }
        return addSchemaLocation(buildDescriptorKey(pluginKey, SETTING_SECTION, descriptorId), location);
    }

    /**
     * Retrieves a schema location by its unique identifier.
     */
    public SchemaLocation getSchemaLocation(String key) {
        return schemaIndex.get(key);
    }

    public SchemaLocation getProfileSchemaLocation(UUID pluginId, String descriptorId) {
        if (pluginId == null || descriptorId == null || descriptorId.isBlank()) {
            return null;
        }
        return getSchemaLocation(buildDescriptorKey(pluginId.toString(), PROFILE_SECTION, descriptorId));
    }

    public SchemaLocation getProfileSchemaLocation(String pluginKey, String descriptorId) {
        if (pluginKey == null || pluginKey.isBlank() || descriptorId == null || descriptorId.isBlank()) {
            return null;
        }
        return getSchemaLocation(buildDescriptorKey(pluginKey, PROFILE_SECTION, descriptorId));
    }

    public SchemaLocation getSettingSchemaLocation(UUID pluginId, String descriptorId) {
        if (pluginId == null || descriptorId == null || descriptorId.isBlank()) {
            return null;
        }
        return getSchemaLocation(buildDescriptorKey(pluginId.toString(), SETTING_SECTION, descriptorId));
    }

    public SchemaLocation getSettingSchemaLocation(String pluginKey, String descriptorId) {
        if (pluginKey == null || pluginKey.isBlank() || descriptorId == null || descriptorId.isBlank()) {
            return null;
        }
        return getSchemaLocation(buildDescriptorKey(pluginKey, SETTING_SECTION, descriptorId));
    }

    public boolean hasSchemaLocation(String key) {
        return schemaIndex.containsKey(key);
    }

    public boolean hasProfileSchema(UUID pluginId, String descriptorId) {
        return getProfileSchemaLocation(pluginId, descriptorId) != null;
    }

    public boolean hasSettingSchema(UUID pluginId, String descriptorId) {
        return getSettingSchemaLocation(pluginId, descriptorId) != null;
    }

    public int getSchemaIndexSize() {
        return schemaIndex.size();
    }

    private String buildDescriptorKey(String base, String section, String descriptorId) {
        return BASE_DESCRIPTOR_KEY_FORMAT.formatted(base, section, descriptorId);
    }

    /**
     * Holds the location of a schema definition, typically the class it's associated with
     * and the path to the schema file.
     */
    public record SchemaLocation(Class<?> clazz, String schemaPath) {
        public SchemaLocation {
            Objects.requireNonNull(clazz, "Class cannot be null");
            if (schemaPath == null) {
                schemaPath = "";
            }
        }

        public boolean hasCustomPath() {
            return !schemaPath.isEmpty();
        }
    }
}
