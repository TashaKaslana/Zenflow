package org.phong.zenflow.user.service;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.user.dtos.UserDto;
import org.phong.zenflow.user.subdomain.permission.dtos.PermissionDto;
import org.phong.zenflow.user.subdomain.permission.infrastructure.mapstruct.PermissionMapper;
import org.phong.zenflow.user.subdomain.role.service.RolePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPermissionService {

    private final UserService userService;
    private final RolePermissionService rolePermissionService;
    private final PermissionMapper permissionMapper;

    /**
     * Get user permissions
     */
    public List<PermissionDto> getUserPermissions(UUID userId) {
        UserDto user = userService.findById(userId);

        if (user.getRole() == null) {
            return List.of();
        }

        return rolePermissionService.findPermissionsByRole(user.getRole().getId()).stream()
                .map(permissionMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Check if user has specific permission
     */
    public boolean userHasPermission(UUID userId, String feature, String action) {
        UserDto user = userService.findById(userId);

        if (user.getRole() == null) {
            return false;
        }

        return rolePermissionService.hasPermission(user.getRole().getId(), feature, action);
    }

    /**
     * Check if user has any of the specified permissions
     */
    public boolean userHasAnyPermission(UUID userId, List<String> features, List<String> actions) {
        UserDto user = userService.findById(userId);
        if (user.getRole() == null) {
            return false;
        }

        for (String feature : features) {
            for (String action : actions) {
                if (rolePermissionService.hasPermission(user.getRole().getId(), feature, action)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if user has all specified permissions
     */
    public boolean userHasAllPermissions(UUID userId, List<String> features, List<String> actions) {
        UserDto user = userService.findById(userId);

        if (user.getRole() == null) {
            return false;
        }

        for (String feature : features) {
            for (String action : actions) {
                if (!rolePermissionService.hasPermission(user.getRole().getId(), feature, action)) {
                    return false;
                }
            }
        }

        return true;
    }
}
