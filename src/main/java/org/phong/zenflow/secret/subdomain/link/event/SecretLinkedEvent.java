package org.phong.zenflow.secret.subdomain.link.event;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;

import java.util.UUID;

public record SecretLinkedEvent(WorkflowDefinition definition, UUID workflowId) {
}
