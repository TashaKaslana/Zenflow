package org.phong.zenflow.secret.exception;

public class SecretDomainException extends RuntimeException {
    public SecretDomainException(String message) {
        super(message);
    }
    public SecretDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
