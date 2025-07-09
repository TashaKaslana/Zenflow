package org.phong.zenflow.user.infrastructure.persistence.projections;

/**
 * Projection for getting only the username field from User
 */
public interface UserUsernameProjection {
    String getUsername();
}
