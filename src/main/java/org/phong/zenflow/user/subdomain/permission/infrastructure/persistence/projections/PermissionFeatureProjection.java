package org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.projections;

/**
 * Projection for getting only the feature field from Permission
 */
public interface PermissionFeatureProjection {
    String getFeature();
}
