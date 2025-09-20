package org.phong.zenflow.plugin.subdomain.registry.profile;

import java.util.Map;

/**
 * Describes an additional field or instruction required to complete profile preparation.
 */
public record ProfilePreparationRequest(
        String fieldKey,
        String label,
        String description,
        boolean secret,
        Map<String, Object> metadata
) {
    public ProfilePreparationRequest {
        if (fieldKey == null || fieldKey.isBlank()) {
            throw new IllegalArgumentException("fieldKey cannot be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label cannot be blank");
        }
        if (metadata == null) {
            metadata = Map.of();
        } else {
            metadata = Map.copyOf(metadata);
        }
    }

    public static ProfilePreparationRequest secretField(
            String fieldKey,
            String label,
            String description,
            Map<String, Object> metadata
    ) {
        return new ProfilePreparationRequest(fieldKey, label, description, true, metadata);
    }
}
