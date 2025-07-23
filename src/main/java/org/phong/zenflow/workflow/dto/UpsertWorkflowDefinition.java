package org.phong.zenflow.workflow.dto;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * DTO for {@link org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow}
 */
public record UpsertWorkflowDefinition(@NotNull List<BaseWorkflowNode> nodes, Map<String, Object> metadata) implements Serializable {
}