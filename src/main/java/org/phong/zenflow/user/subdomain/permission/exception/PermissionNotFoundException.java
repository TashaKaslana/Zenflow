package org.phong.zenflow.user.subdomain.permission.exception;

import org.phong.zenflow.user.subdomain.permission.enums.PermissionError;

/**
 * Exception thrown when a permission is not found
 */
public class PermissionNotFoundException extends PermissionDomainException {
    public PermissionNotFoundException(String permissionId) {
        super(PermissionError.PERMISSION_NOT_FOUND, permissionId);
    }

    public PermissionNotFoundException(String feature, String action) {
        super(PermissionError.PERMISSION_NOT_FOUND, "feature: " + feature + ", action: " + action);
    }
}
