package org.phong.zenflow.workflow.subdomain.workflow_version.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;

/**
 * DTO for {@link org.phong.zenflow.workflow.subdomain.workflow_version.infrastructure.persistence.entity.WorkflowVersion}
 */
public record WorkflowVersionDto(
        @NotNull UUID id,
        @NotNull UUID workflowId,
        @NotNull Integer version,
        WorkflowDefinition definition,
        Boolean isAutosave,
        UUID createdBy,
        @NotNull OffsetDateTime createdAt
) implements Serializable {
}

