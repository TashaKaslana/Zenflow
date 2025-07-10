package org.phong.zenflow.user.exception;

import org.phong.zenflow.user.enums.UserError;

/**
 * Exception thrown when attempting to create a user with an email that already exists
 */
public class UserEmailExistsException extends UserDomainException {
    public UserEmailExistsException(String email) {
        super(UserError.USER_EMAIL_EXISTS, email);
    }
}
