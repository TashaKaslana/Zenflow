package org.phong.zenflow.workflow.subdomain.node_execution.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.persistence.entity.NodeExecution}
 **/
public record CreateNodeExecutionRequest(UUID workflowRunId, @NotNull String nodeKey) implements Serializable {
}