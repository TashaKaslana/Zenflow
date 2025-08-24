package org.phong.zenflow.workflow.subdomain.logging.api.exception;

public class NodeLogException extends RuntimeException {
    public NodeLogException(String message) {
        super(message);
    }

    public NodeLogException(String message, Throwable cause) {
        super(message, cause);
    }
}
