package org.phong.zenflow.secret.exception;

public class SecretNotFoundException extends SecretDomainException{
    public SecretNotFoundException(String message) {
        super(message);
    }
}
