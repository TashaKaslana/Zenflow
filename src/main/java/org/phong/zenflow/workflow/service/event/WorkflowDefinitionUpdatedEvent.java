package org.phong.zenflow.workflow.service.event;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;

import java.util.UUID;

public record WorkflowDefinitionUpdatedEvent(UUID workflowId, WorkflowDefinition definition) {
}
