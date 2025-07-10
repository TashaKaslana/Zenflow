package org.phong.zenflow.user.exception;

import org.phong.zenflow.user.enums.UserError;

/**
 * Exception thrown when a user entity is not found
 */
public class UserNotFoundException extends UserDomainException {

    /**
     * Constructor with user ID
     *
     * @param userId The ID of the user that was not found
     */
    public UserNotFoundException(String userId) {
        super(UserError.USER_NOT_FOUND, userId);
    }

    /**
     * Constructor with property name and value
     *
     * @param property The property name (e.g., "email", "username")
     * @param value The value of the property that was not found
     */
    public UserNotFoundException(String property, String value) {
        super(UserError.USER_NOT_FOUND, property + ": " + value);
    }
}
