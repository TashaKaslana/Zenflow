package org.phong.zenflow.secret.subdomain.profile.projection;

import org.phong.zenflow.secret.subdomain.profile.entity.SecretProfile;

import java.util.UUID;

/**
 * Projection for {@link SecretProfile}
 */
public interface SecretProfileInfo {
    UUID getId();
}