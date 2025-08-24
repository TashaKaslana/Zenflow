package org.phong.zenflow.workflow.subdomain.node_execution.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * DTO for {@link org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.persistence.entity.NodeExecution}
 */
public record UpdateNodeExecutionRequest(@NotNull String nodeKey, @NotNull String status, String error,
                                         Integer attempts, Map<String, Object> output, OffsetDateTime endedAt) implements Serializable {
}