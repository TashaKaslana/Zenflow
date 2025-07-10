package org.phong.zenflow.user.subdomain.role.exception;

import org.phong.zenflow.user.subdomain.role.enums.RoleError;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;

/**
 * Exception thrown when a role is not found
 */
public class RoleNotFoundException extends RoleDomainException {
    public RoleNotFoundException(String roleId) {
        super(RoleError.ROLE_NOT_FOUND, roleId);
    }

    public RoleNotFoundException(String property, String value) {
        super(RoleError.ROLE_NOT_FOUND, property + ": " + value);
    }

    public RoleNotFoundException(String property, UserRoleEnum value) {
        super(RoleError.ROLE_NOT_FOUND, property + ": " + value.name());
    }
}
