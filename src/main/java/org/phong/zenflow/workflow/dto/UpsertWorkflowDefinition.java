package org.phong.zenflow.workflow.dto;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow}
 */
public record UpsertWorkflowDefinition(@NotNull List<BaseWorkflowNode> nodes, WorkflowMetadata metadata) implements Serializable {
}