package org.phong.zenflow.user.subdomain.permission.exception;

import lombok.Getter;
import org.phong.zenflow.user.subdomain.permission.enums.PermissionError;

/**
 * Base exception class for permission subdomain exceptions
 */
@Getter
public class PermissionDomainException extends RuntimeException {
    private final PermissionError error;

    public PermissionDomainException(PermissionError error, String details) {
        super(error.formatMessage(details));
        this.error = error;
    }
}
