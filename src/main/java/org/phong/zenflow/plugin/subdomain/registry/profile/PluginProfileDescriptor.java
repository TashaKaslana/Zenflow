package org.phong.zenflow.plugin.subdomain.registry.profile;

import java.util.Map;

/**
 * Describes a single credential profile that a plugin can expose.
 * Each descriptor may optionally provide a JSON schema and a preparation hook.
 */
public interface PluginProfileDescriptor {

    /**
     * Stable identifier unique within the owning plugin.
     */
    String id();

    /**
     * Human readable label presented to users when choosing a profile type.
     */
    String displayName();

    /**
     * Optional short description explaining what the profile is used for.
     */
    default String description() {
        return "";
    }

    /**
     * Relative path to the JSON schema describing user supplied fields for this profile.
     * Paths are resolved relative to the plugin definition package. Implementations may
     * return an empty string when no schema is required.
     */
    default String schemaPath() {
        return "";
    }

    /**
     * Provide default values that should be pre-populated when rendering the profile form.
     */
    default Map<String, Object> defaultValues() {
        return Map.of();
    }

    /**
     * Whether the descriptor requires an additional preparation step after the user submits
     * the form (e.g., exchanging credentials for a refresh token).
     */
    default boolean requiresPreparation() {
        return false;
    }

    /**
     * Perform profile preparation such as generating additional secrets. The context exposes
     * user supplied values and allows storing generated secrets back into the profile.
     */
    default void prepareProfile(ProfilePreparationContext context) {
        // Default implementations do not require preparation.
    }
}
