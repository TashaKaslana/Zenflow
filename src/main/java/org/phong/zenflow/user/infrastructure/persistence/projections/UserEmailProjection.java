package org.phong.zenflow.user.infrastructure.persistence.projections;

/**
 * Projection for getting only the email field from User
 */
public interface UserEmailProjection {
    String getEmail();
}
