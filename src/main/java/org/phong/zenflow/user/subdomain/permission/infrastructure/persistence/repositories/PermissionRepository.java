package org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.repositories;

import org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.entities.Permission;
import org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.projections.PermissionActionProjection;
import org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.projections.PermissionFeatureProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    // Bulk queries with projections for efficient data retrieval
    @Query("SELECT DISTINCT p.feature FROM Permission p")
    List<PermissionFeatureProjection> findAllFeatures();

    @Query("SELECT DISTINCT p.action FROM Permission p")
    List<PermissionActionProjection> findAllActions();

    // Find by feature and action
    @Query("SELECT p FROM Permission p WHERE p.feature = :feature AND p.action = :action")
    Optional<Permission> findByFeatureAndAction(@Param("feature") String feature, @Param("action") String action);

    // Find by feature
    @Query("SELECT p FROM Permission p WHERE p.feature = :feature")
    List<Permission> findByFeature(@Param("feature") String feature);

    // Find by action
    @Query("SELECT p FROM Permission p WHERE p.action = :action")
    List<Permission> findByAction(@Param("action") String action);

    // Check existence by feature and action
    boolean existsByFeatureAndAction(String feature, String action);

    // Bulk operations
    @Query("SELECT p FROM Permission p WHERE p.feature IN :features")
    List<Permission> findByFeatureIn(@Param("features") List<String> features);

    @Query("SELECT p FROM Permission p WHERE p.action IN :actions")
    List<Permission> findByActionIn(@Param("actions") List<String> actions);

    @Query("SELECT p FROM Permission p WHERE p.id IN :ids")
    List<Permission> findByIdIn(@Param("ids") List<UUID> ids);
}