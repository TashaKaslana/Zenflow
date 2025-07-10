package org.phong.zenflow.user.subdomain.permission.enums;

import lombok.Getter;

@Getter
public enum PermissionError {
    PERMISSION_NOT_FOUND("PERMISSION_NOT_FOUND", "Permission not found"),
    PERMISSION_LIST_NOT_FOUND("PERMISSION_LIST_NOT_FOUND", "Permission list not found"),
    PERMISSION_ALREADY_EXISTS("PERMISSION_ALREADY_EXISTS", "Permission already exists"),
    INVALID_PERMISSION_ACTION("INVALID_PERMISSION_ACTION", "Invalid permission action");

    private final String code;
    private final String message;

    PermissionError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String formatMessage(String details) {
        return String.format("%s: %s", message, details);
    }
}