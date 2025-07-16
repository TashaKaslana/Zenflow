package org.phong.zenflow.workflow.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO for {@link org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow}
 */
public record UpdateWorkflowRequest(@NotNull String name, String description, Map<String, Object> definition, String startNode,
                                    @NotNull Boolean isActive, Map<String, Object> retryPolicy) implements Serializable {
}