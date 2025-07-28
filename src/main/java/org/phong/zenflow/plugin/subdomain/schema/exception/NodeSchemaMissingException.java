package org.phong.zenflow.plugin.subdomain.schema.exception;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class NodeSchemaMissingException extends RuntimeException {
    private final List<UUID> missingFields;

    public NodeSchemaMissingException(String message, List<UUID> missingFields) {
        super(message);
        this.missingFields = missingFields;
    }
}
