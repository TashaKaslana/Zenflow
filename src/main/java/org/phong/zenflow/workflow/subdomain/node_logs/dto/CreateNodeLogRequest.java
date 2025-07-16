package org.phong.zenflow.workflow.subdomain.node_logs.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.entity.NodeLog}
 */
public record CreateNodeLogRequest(UUID workflowRunId, @NotNull String nodeKey, @NotNull String status, String error,
                                   Integer attempts, Map<String, Object> output,
                                   Map<String, Object> logs) implements Serializable {
}