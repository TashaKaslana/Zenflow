package org.phong.zenflow.workflow.subdomain.node_logs.exception;

public class NodeLogException extends RuntimeException {
    public NodeLogException(String message) {
        super(message);
    }

    public NodeLogException(String message, Throwable cause) {
        super(message, cause);
    }
}
