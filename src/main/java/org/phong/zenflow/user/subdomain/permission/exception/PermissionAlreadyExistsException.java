package org.phong.zenflow.user.subdomain.permission.exception;

import org.phong.zenflow.user.subdomain.permission.enums.PermissionError;

/**
 * Exception thrown when a permission already exists
 */
public class PermissionAlreadyExistsException extends PermissionDomainException {
    public PermissionAlreadyExistsException(String feature, String action) {
        super(PermissionError.PERMISSION_ALREADY_EXISTS, "feature: " + feature + ", action: " + action);
    }
}
