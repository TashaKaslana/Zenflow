package org.phong.zenflow.plugin.subdomain.schema.exception;

public class NodeSchemaException extends RuntimeException {
    public NodeSchemaException(String message) {
        super(message);
    }

    public NodeSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
