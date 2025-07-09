package org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.projections;

/**
 * Projection for getting only the action field from Permission
 */
public interface PermissionActionProjection {
    String getAction();
}
