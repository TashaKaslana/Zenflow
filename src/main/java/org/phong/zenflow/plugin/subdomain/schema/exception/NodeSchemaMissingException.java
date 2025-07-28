package org.phong.zenflow.plugin.subdomain.schema.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class NodeSchemaMissingException extends RuntimeException {
    private final List<String> missingFields;

    public NodeSchemaMissingException(String message, List<String> missingFields) {
        super(message);
        this.missingFields = missingFields;
    }
}
