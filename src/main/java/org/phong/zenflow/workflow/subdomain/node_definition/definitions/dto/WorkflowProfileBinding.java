package org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto;

import java.io.Serializable;
import java.util.UUID;

/**
 * Captures the relationship between a workflow node and the plugin profile it consumes.
 */
public record WorkflowProfileBinding(
        String pluginKey,
        String profileKey,
        String profileName,
        UUID profileId
) implements Serializable {
    public WorkflowProfileBinding {
        pluginKey = normalize(pluginKey);
        profileKey = normalize(profileKey);
        profileName = normalize(profileName);
    }

    public WorkflowProfileBinding(String pluginKey, String profileKey) {
        this(pluginKey, profileKey, null, null);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
