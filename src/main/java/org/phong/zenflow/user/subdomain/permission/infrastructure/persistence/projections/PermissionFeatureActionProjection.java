package org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.projections;

/**
 * Projection for getting feature and action fields from Permission
 */
public interface PermissionFeatureActionProjection {
    String getFeature();
    String getAction();
}
