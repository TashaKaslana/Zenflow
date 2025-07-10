package org.phong.zenflow.user.subdomain.role.exception;

import org.phong.zenflow.user.subdomain.role.enums.RoleError;

/**
 * Exception thrown when attempting to assign a permission that is already assigned to a role
 */
public class PermissionAlreadyAssignedException extends RoleDomainException {

    /**
     * Constructor with role ID and permission ID
     *
     * @param roleId The ID of the role
     * @param permissionId The ID of the permission
     */
    public PermissionAlreadyAssignedException(String roleId, String permissionId) {
        super(RoleError.PERMISSION_ALREADY_ASSIGNED, "roleId: " + roleId + ", permissionId: " + permissionId);
    }
}
