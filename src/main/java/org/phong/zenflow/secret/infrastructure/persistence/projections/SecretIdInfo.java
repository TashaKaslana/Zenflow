package org.phong.zenflow.secret.infrastructure.persistence.projections;

import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;

import java.util.UUID;

/**
 * Projection for {@link Secret}
 */
public interface SecretIdInfo {
    UUID getId();
}