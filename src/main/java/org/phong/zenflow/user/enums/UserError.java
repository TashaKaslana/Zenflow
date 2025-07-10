package org.phong.zenflow.user.enums;

import lombok.Getter;

/**
 * Enumeration of user-specific error messages
 */
@Getter
public enum UserError {
    USER_NOT_FOUND("user.not_found", "User not found"),
    USER_EMAIL_EXISTS("user.email_exists", "Email already exists"),
    USER_USERNAME_EXISTS("user.username_exists", "Username already exists"),
    USER_INVALID_CREDENTIALS("user.invalid_credentials", "Invalid credentials");

    private final String code;
    private final String message;

    UserError(String code, String message) {
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
