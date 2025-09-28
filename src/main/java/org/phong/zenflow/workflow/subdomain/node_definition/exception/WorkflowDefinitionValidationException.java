package org.phong.zenflow.workflow.subdomain.node_definition.exception;

import lombok.Getter;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;

@Getter
public class WorkflowDefinitionValidationException extends WorkflowNodeDefinitionException {
    private final ValidationResult validationResult;

    public WorkflowDefinitionValidationException(String message, ValidationResult validationResult) {
        super(message);
        this.validationResult = validationResult;
    }
}
