package org.phong.zenflow.user.exception;

import org.phong.zenflow.user.enums.UserError;

/**
 * Exception thrown when attempting to create a user with a username that already exists
 */
public class UserUsernameExistsException extends UserDomainException {
    public UserUsernameExistsException(String username) {
        super(UserError.USER_USERNAME_EXISTS, username);
    }
}
