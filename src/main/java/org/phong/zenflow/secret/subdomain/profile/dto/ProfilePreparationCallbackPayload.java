package org.phong.zenflow.secret.subdomain.profile.dto;

import org.phong.zenflow.plugin.subdomain.registry.profile.ProfilePreparationRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Payload sent to external systems when profile preparation requires additional input.
 */
public record ProfilePreparationCallbackPayload(
        UUID pluginId,
        UUID pluginNodeId,
        Map<String, String> submittedValues,
        List<ProfilePreparationRequest> pendingRequests
) {
}
