package org.phong.zenflow.plugin.subdomain.registry.settings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds runtime metadata for a registered plugin setting descriptor.
 */
public record RegisteredPluginSettingDescriptor(
        PluginSettingDescriptor descriptor,
        Map<String, Object> schema,
        Map<String, Object> defaultValues
) {
    public RegisteredPluginSettingDescriptor {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor cannot be null");
        }
        schema = schema == null ? Map.of() : Map.copyOf(schema);
        defaultValues = defaultValues == null ? Map.of() : Map.copyOf(defaultValues);
    }

    public Map<String, Object> asMetadataMap() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", descriptor.id());
        metadata.put("label", descriptor.displayName());
        if (!descriptor.description().isBlank()) {
            metadata.put("description", descriptor.description());
        }
        if (!defaultValues.isEmpty()) {
            metadata.put("defaults", defaultValues);
        }
        if (!schema.isEmpty()) {
            metadata.put("schema", schema);
        }
        return metadata;
    }
}
