package org.phong.zenflow.user.subdomain.role.enums;

import lombok.Getter;

/**
 * Enumeration of role-specific error messages
 */
@Getter
public enum RoleError {
    ROLE_NOT_FOUND("role.not_found", "Role not found"),
    ROLE_NAME_EXISTS("role.name_exists", "Role name already exists"),
    PERMISSION_ALREADY_ASSIGNED("role.permission_already_assigned", "Permission already assigned to role"),
    PERMISSION_NOT_ASSIGNED("role.permission_not_assigned", "Permission not assigned to role");

    private final String code;
    private final String message;

    RoleError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Format error message with additional details
     * @param details Additional details to append to the message
     * @return Formatted error message
     */
    public String formatMessage(String details) {
        return this.message + ": " + details;
    }
}
