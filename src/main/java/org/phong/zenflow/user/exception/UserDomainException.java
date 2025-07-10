package org.phong.zenflow.user.exception;

import org.phong.zenflow.user.enums.UserError;
import lombok.Getter;

/**
 * Base exception class for all user domain exceptions
 */
@Getter
public class UserDomainException extends RuntimeException {
    private final UserError error;

    public UserDomainException(UserError error) {
        super(error.getMessage());
        this.error = error;
    }

    public UserDomainException(UserError error, String details) {
        super(error.formatMessage(details));
        this.error = error;
    }
}
