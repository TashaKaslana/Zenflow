package org.phong.zenflow.workflow.dto;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;

import java.time.OffsetDateTime;

public record UpdateWorkflowDefinitionResponse(
        WorkflowDefinition definition,
        Boolean isActive,
        ValidationResult validation,
        boolean publishAttempt,
        OffsetDateTime validatedAt
) {}
