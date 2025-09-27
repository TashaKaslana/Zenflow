package org.phong.zenflow.plugin.subdomain.registry.settings;

import java.util.Map;

/**
 * Describes a plugin-level configuration section (e.g., shared settings) exposed by a plugin.
 */
public interface PluginSettingDescriptor {

    /**
     * Stable identifier unique within the owning plugin.
     */
    String id();

    /**
     * Human readable label presented to users when configuring the section.
     */
    String displayName();

    /**
     * Optional description that explains what the settings control.
     */
    default String description() {
        return "";
    }

    /**
     * Relative or absolute path to the JSON schema describing this setting block.
     */
    default String schemaPath() {
        return "";
    }

    /**
     * Default values to pre-populate when rendering the configuration form.
     */
    default Map<String, Object> defaultValues() {
        return Map.of();
    }
}
