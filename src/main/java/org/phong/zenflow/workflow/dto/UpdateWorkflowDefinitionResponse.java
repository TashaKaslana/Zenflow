package org.phong.zenflow.workflow.dto;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;

public record UpdateWorkflowDefinitionResponse(
        WorkflowDefinition definition,
        Boolean isActive,
        ValidationResult definitionValidation,
        ValidationResult publishValidation
) {}
