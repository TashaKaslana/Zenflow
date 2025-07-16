package org.phong.zenflow.workflow.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow}
 */
public record CreateWorkflowRequest(UUID projectId, @NotNull String name, String description, Map<String, Object> definition,
                                    String startNode, Map<String, Object> retryPolicy) implements Serializable {
}