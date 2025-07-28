package org.phong.zenflow.workflow.subdomain.engine.exception;

import lombok.Getter;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;

@Getter
public class WorkflowEngineValidationException extends WorkflowEngineException {
    private final ValidationResult validationResult;
    private final String nodeKey;

    public WorkflowEngineValidationException(String message, String nodeKey, ValidationResult validationResult) {
        super(message);
        this.validationResult = validationResult;
        this.nodeKey = nodeKey;
    }
}
