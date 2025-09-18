package org.phong.zenflow.workflow.service.dto;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;

public record WorkflowDefinitionUpdateResult(
        WorkflowDefinition definition,
        ValidationResult validation,
        boolean publishAttempt,
        boolean isActive
) {
}
