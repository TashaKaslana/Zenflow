package org.phong.zenflow.secret.subdomain.link.infrastructure.projection;

import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretProfileNodeLink;

import java.util.UUID;

/**
 * Projection for {@link SecretProfileNodeLink}
 */
public interface SecretProfileNodeLinkInfo {
    UUID getId();

    UUID getProfileId();

    String getNodeKey();
}