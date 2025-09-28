package org.phong.zenflow.secret.subdomain.profile.dto;

import org.phong.zenflow.plugin.subdomain.registry.profile.ProfilePreparationRequest;

import java.util.List;

/**
 * Result returned to the REST layer after attempting to create a profile secret.
 */
public record ProfileSecretCreationResult(
        boolean pending,
        ProfileSecretListDto profileSecrets,
        List<ProfilePreparationRequest> pendingRequests
) {

    public ProfileSecretCreationResult {
        if (pending && profileSecrets != null) {
            throw new IllegalArgumentException("Pending result cannot include profile secrets");
        }
        pendingRequests = pendingRequests == null ? List.of() : List.copyOf(pendingRequests);
    }

    public static ProfileSecretCreationResult pending(List<ProfilePreparationRequest> requests) {
        return new ProfileSecretCreationResult(true, null, requests);
    }

    public static ProfileSecretCreationResult completed(ProfileSecretListDto profileSecrets) {
        return new ProfileSecretCreationResult(false, profileSecrets, List.of());
    }

    public boolean hasPendingRequests() {
        return pending;
    }
}
