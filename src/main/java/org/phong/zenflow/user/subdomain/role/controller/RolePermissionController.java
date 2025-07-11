package org.phong.zenflow.user.subdomain.role.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.entities.Permission;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.entities.Role;
import org.phong.zenflow.user.subdomain.role.service.RolePermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @PostMapping("/assign")
    public ResponseEntity<RestApiResponse<Void>> assignPermissionToRole(
            @RequestParam UUID roleId,
            @RequestParam UUID permissionId) {
        rolePermissionService.assignPermissionToRole(roleId, permissionId);
        return RestApiResponse.success("Permission assigned to role successfully");
    }

    @PostMapping("/assign-bulk")
    public ResponseEntity<RestApiResponse<Void>> assignPermissionsToRole(
            @RequestParam UUID roleId,
            @RequestBody List<UUID> permissionIds) {
        rolePermissionService.assignPermissionsToRole(roleId, permissionIds);
        return RestApiResponse.success("Permissions assigned to role successfully");
    }

    @DeleteMapping("/remove")
    public ResponseEntity<RestApiResponse<Void>> removePermissionFromRole(
            @RequestParam UUID roleId,
            @RequestParam UUID permissionId) {
        rolePermissionService.removePermissionFromRole(roleId, permissionId);
        return RestApiResponse.success("Permission removed from role successfully");
    }

    @DeleteMapping("/remove-bulk")
    public ResponseEntity<RestApiResponse<Void>> removePermissionsFromRole(
            @RequestParam UUID roleId,
            @RequestBody List<UUID> permissionIds) {
        rolePermissionService.removePermissionsFromRole(roleId, permissionIds);
        return RestApiResponse.success("Permissions removed from role successfully");
    }

    @DeleteMapping("/remove-all")
    public ResponseEntity<RestApiResponse<Void>> removeAllPermissionsFromRole(@RequestParam UUID roleId) {
        rolePermissionService.removeAllPermissionsFromRole(roleId);
        return RestApiResponse.success("All permissions removed from role successfully");
    }

    @GetMapping("/role/{roleId}/permissions")
    public ResponseEntity<RestApiResponse<List<Permission>>> getPermissionsByRole(@PathVariable UUID roleId) {
        List<Permission> permissions = rolePermissionService.findPermissionsByRole(roleId);
        return RestApiResponse.success(permissions, "Role permissions retrieved successfully");
    }

    @GetMapping("/permission/{permissionId}/roles")
    public ResponseEntity<RestApiResponse<List<Role>>> getRolesByPermission(@PathVariable UUID permissionId) {
        List<Role> roles = rolePermissionService.findRolesByPermission(permissionId);
        return RestApiResponse.success(roles, "Permission roles retrieved successfully");
    }

    @GetMapping("/check")
    public ResponseEntity<RestApiResponse<Boolean>> checkPermissionAssignedToRole(
            @RequestParam UUID roleId,
            @RequestParam UUID permissionId) {
        boolean assigned = rolePermissionService.isPermissionAssignedToRole(roleId, permissionId);
        return RestApiResponse.success(assigned, "Permission assignment checked");
    }

    @GetMapping("/check-feature-action")
    public ResponseEntity<RestApiResponse<Boolean>> checkRoleHasPermission(
            @RequestParam UUID roleId,
            @RequestParam String feature,
            @RequestParam String action) {
        boolean hasPermission = rolePermissionService.hasPermission(roleId, feature, action);
        return RestApiResponse.success(hasPermission, "Role permission checked");
    }

    @GetMapping("/role/{roleId}/permissions/count")
    public ResponseEntity<RestApiResponse<Long>> countPermissionsByRole(@PathVariable UUID roleId) {
        long count = rolePermissionService.countPermissionsByRole(roleId);
        return RestApiResponse.success(count, "Permission count for role retrieved successfully");
    }

    @GetMapping("/permission/{permissionId}/roles/count")
    public ResponseEntity<RestApiResponse<Long>> countRolesByPermission(@PathVariable UUID permissionId) {
        long count = rolePermissionService.countRolesByPermission(permissionId);
        return RestApiResponse.success(count, "Role count for permission retrieved successfully");
    }

    @GetMapping("/count")
    public ResponseEntity<RestApiResponse<Long>> getTotalRolePermissionCount() {
        long count = rolePermissionService.count();
        return RestApiResponse.success(count, "Total role-permission associations retrieved successfully");
    }
}
