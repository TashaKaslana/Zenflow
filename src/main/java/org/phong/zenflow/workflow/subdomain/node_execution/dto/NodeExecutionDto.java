package org.phong.zenflow.workflow.subdomain.node_execution.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.persistence.entity.NodeExecution}
 */
public record NodeExecutionDto(@NotNull UUID id, UUID workflowRunId, @NotNull String nodeKey, @NotNull String status,
                               String error, Integer attempts, Map<String, Object> output,
                               @NotNull OffsetDateTime startedAt, OffsetDateTime endedAt) implements Serializable {
}