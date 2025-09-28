package org.phong.zenflow.secret.subdomain.profile.dto;

import org.phong.zenflow.plugin.subdomain.registry.profile.ProfilePreparationRequest;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates the outcome of running profile preparation hooks.
 */
public record ProfilePreparationResult(
        Map<String, String> preparedSecrets,
        List<ProfilePreparationRequest> pendingRequests
) {

    public ProfilePreparationResult {
        preparedSecrets = preparedSecrets == null ? Map.of() : Map.copyOf(preparedSecrets);
        pendingRequests = pendingRequests == null ? List.of() : List.copyOf(pendingRequests);
    }

    public boolean isReady() {
        return pendingRequests.isEmpty();
    }
}
