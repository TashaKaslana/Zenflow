package org.phong.zenflow.workflow.dto;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO for {@link org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow}
 */
public record UpdateWorkflowRequest(String name, String description, WorkflowDefinition definition,
                                    String startNode,
                                    Boolean isActive,
                                    Map<String, Object> retryPolicy) implements Serializable {
}