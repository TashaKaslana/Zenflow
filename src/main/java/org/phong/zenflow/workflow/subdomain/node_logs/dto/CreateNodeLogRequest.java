package org.phong.zenflow.workflow.subdomain.node_logs.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.entity.NodeLog}
 */
public record CreateNodeLogRequest(UUID workflowRunId, @NotNull String nodeKey) implements Serializable {
}