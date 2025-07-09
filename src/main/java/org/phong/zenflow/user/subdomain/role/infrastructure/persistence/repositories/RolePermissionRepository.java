package org.phong.zenflow.user.subdomain.role.infrastructure.persistence.repositories;

import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.entities.RolePermission;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.key.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    // Bulk operations for better performance
    @Query("SELECT rp FROM RolePermission rp WHERE rp.id.roleId = :roleId")
    List<RolePermission> findByRoleId(@Param("roleId") UUID roleId);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.id.permissionId = :permissionId")
    List<RolePermission> findByPermissionId(@Param("permissionId") UUID permissionId);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.id.roleId IN :roleIds")
    List<RolePermission> findByRoleIdIn(@Param("roleIds") List<UUID> roleIds);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.id.permissionId IN :permissionIds")
    List<RolePermission> findByPermissionIdIn(@Param("permissionIds") List<UUID> permissionIds);

    @Query("DELETE FROM RolePermission rp WHERE rp.id.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") UUID roleId);

    @Query("DELETE FROM RolePermission rp WHERE rp.id.permissionId = :permissionId")
    void deleteByPermissionId(@Param("permissionId") UUID permissionId);
}