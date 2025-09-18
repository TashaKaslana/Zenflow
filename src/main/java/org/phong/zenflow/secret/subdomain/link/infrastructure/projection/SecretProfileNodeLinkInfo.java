package org.phong.zenflow.secret.subdomain.link.infrastructure.projection;

import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretProfileNodeLink;
import org.phong.zenflow.secret.subdomain.profile.projection.SecretProfileInfo;

import java.util.UUID;

/**
 * Projection for {@link SecretProfileNodeLink}
 */
public interface SecretProfileNodeLinkInfo {
    UUID getId();

    SecretProfileInfo getProfile();

    String getNodeKey();

    default UUID getProfileId() {
        return getProfile().getId();
    }
}