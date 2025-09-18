package org.phong.zenflow.secret.subdomain.link.infrastructure.projection;

import org.phong.zenflow.secret.infrastructure.persistence.projections.SecretIdInfo;
import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretNodeLink;

import java.util.UUID;

/**
 * Projection for {@link SecretNodeLink}
 */
public interface SecretNodeLinkInfo {
    UUID getId();

    SecretIdInfo getSecret();

    String getNodeKey();

    default UUID getSecretId() {
        return getSecret().getId();
    }
}