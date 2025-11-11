package org.phong.zenflow.secret.subdomain.link.dto;

import java.util.UUID;

/**
 * Payload describing an existing profile-node link that should target a different profile.
 */
public record ProfileLinkUpdateRequest(UUID linkId, UUID profileId) {
}
