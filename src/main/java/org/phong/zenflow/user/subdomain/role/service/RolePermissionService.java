package org.phong.zenflow.user.subdomain.role.service;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.entities.Permission;
import org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.repositories.PermissionRepository;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.entities.Role;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.entities.RolePermission;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.key.RolePermissionId;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.repositories.RolePermissionRepository;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.repositories.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Assign permission to role
     */
    @Transactional
    public RolePermission assignPermissionToRole(UUID roleId, UUID permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));

        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new IllegalArgumentException("Permission not found with id: " + permissionId));

        RolePermissionId id = new RolePermissionId();
        id.setRoleId(roleId);
        id.setPermissionId(permissionId);

        // Check if the assignment already exists
        if (rolePermissionRepository.existsById(id)) {
            throw new IllegalArgumentException("Permission already assigned to role");
        }

        RolePermission rolePermission = new RolePermission();
        rolePermission.setId(id);
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);

        return rolePermissionRepository.save(rolePermission);
    }

    /**
     * Remove permission from role
     */
    @Transactional
    public void removePermissionFromRole(UUID roleId, UUID permissionId) {
        RolePermissionId id = new RolePermissionId();
        id.setRoleId(roleId);
        id.setPermissionId(permissionId);

        if (!rolePermissionRepository.existsById(id)) {
            throw new IllegalArgumentException("Permission not assigned to role");
        }

        rolePermissionRepository.deleteById(id);
    }

    /**
     * Find role permission by composite ID
     */
    public Optional<RolePermission> findById(UUID roleId, UUID permissionId) {
        RolePermissionId id = new RolePermissionId();
        id.setRoleId(roleId);
        id.setPermissionId(permissionId);

        return rolePermissionRepository.findById(id);
    }

    /**
     * Find all role permissions
     */
    public List<RolePermission> findAll() {
        return rolePermissionRepository.findAll();
    }

    /**
     * Check if permission is assigned to role
     */
    public boolean isPermissionAssignedToRole(UUID roleId, UUID permissionId) {
        RolePermissionId id = new RolePermissionId();
        id.setRoleId(roleId);
        id.setPermissionId(permissionId);

        return rolePermissionRepository.existsById(id);
    }

    /**
     * Find all permissions for a role using bulk query
     */
    public List<Permission> findPermissionsByRole(UUID roleId) {
        return rolePermissionRepository.findByRoleId(roleId).stream()
            .map(RolePermission::getPermission)
            .toList();
    }

    /**
     * Find all roles that have a specific permission using bulk query
     */
    public List<Role> findRolesByPermission(UUID permissionId) {
        return rolePermissionRepository.findByPermissionId(permissionId).stream()
            .map(RolePermission::getRole)
            .toList();
    }

    /**
     * Assign multiple permissions to a role in bulk
     */
    @Transactional
    public void assignPermissionsToRole(UUID roleId, List<UUID> permissionIds) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));

        // Get existing permissions for this role
        List<UUID> existingPermissionIds = rolePermissionRepository.findByRoleId(roleId).stream()
            .map(rp -> rp.getId().getPermissionId())
            .toList();

        // Filter out already assigned permissions
        List<UUID> newPermissionIds = permissionIds.stream()
            .filter(permissionId -> !existingPermissionIds.contains(permissionId))
            .toList();

        if (!newPermissionIds.isEmpty()) {
            // Verify all permissions exist
            List<Permission> permissions = permissionRepository.findByIdIn(newPermissionIds);
            if (permissions.size() != newPermissionIds.size()) {
                throw new IllegalArgumentException("Some permissions not found");
            }

            // Create role permissions in bulk
            List<RolePermission> rolePermissions = permissions.stream()
                .map(permission -> {
                    RolePermissionId id = new RolePermissionId();
                    id.setRoleId(roleId);
                    id.setPermissionId(permission.getId());

                    RolePermission rolePermission = new RolePermission();
                    rolePermission.setId(id);
                    rolePermission.setRole(role);
                    rolePermission.setPermission(permission);
                    return rolePermission;
                })
                .toList();

            rolePermissionRepository.saveAll(rolePermissions);
        }
    }

    /**
     * Remove multiple permissions from a role in bulk
     */
    @Transactional
    public void removePermissionsFromRole(UUID roleId, List<UUID> permissionIds) {
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId).stream()
            .filter(rp -> permissionIds.contains(rp.getId().getPermissionId()))
            .toList();

        if (!rolePermissions.isEmpty()) {
            rolePermissionRepository.deleteAll(rolePermissions);
        }
    }

    /**
     * Remove all permissions from a role using bulk delete
     */
    @Transactional
    public void removeAllPermissionsFromRole(UUID roleId) {
        rolePermissionRepository.deleteByRoleId(roleId);
    }

    /**
     * Check if role has specific permission by feature and action
     */
    public boolean hasPermission(UUID roleId, String feature, String action) {
        List<Permission> permissions = findPermissionsByRole(roleId);
        return permissions.stream()
            .anyMatch(permission -> permission.getFeature().equals(feature) &&
                                   permission.getAction().equals(action));
    }

    /**
     * Count permissions for a role
     */
    public long countPermissionsByRole(UUID roleId) {
        return rolePermissionRepository.findByRoleId(roleId).size();
    }

    /**
     * Count roles that have a specific permission
     */
    public long countRolesByPermission(UUID permissionId) {
        return rolePermissionRepository.findByPermissionId(permissionId).size();
    }

    /**
     * Count all role permissions
     */
    public long count() {
        return rolePermissionRepository.count();
    }
}
