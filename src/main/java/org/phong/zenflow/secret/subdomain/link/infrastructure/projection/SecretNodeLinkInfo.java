package org.phong.zenflow.secret.subdomain.link.infrastructure.projection;

import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretNodeLink;

import java.util.UUID;

/**
 * Projection for {@link SecretNodeLink}
 */
public interface SecretNodeLinkInfo {
    UUID getId();

    UUID getSecretId();

    String getNodeKey();
}