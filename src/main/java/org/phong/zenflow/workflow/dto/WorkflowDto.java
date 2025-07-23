package org.phong.zenflow.workflow.dto;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow}
 */
public record WorkflowDto(@NotNull UUID id, @NotNull OffsetDateTime createdAt, @NotNull OffsetDateTime updatedAt,
                          UUID createdBy, UUID updatedBy, UUID projectId, @NotNull String name,
                          WorkflowDefinition definition, String startNode, @NotNull Boolean isActive,
                          OffsetDateTime deletedAt, String description,
                          Map<String, Object> retryPolicy) implements Serializable {
}