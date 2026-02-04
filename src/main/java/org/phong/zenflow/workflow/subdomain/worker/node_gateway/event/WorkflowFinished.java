package org.phong.zenflow.workflow.subdomain.worker.node_gateway.event;

import org.phong.zenflow.workflow.subdomain.worker.node_gateway.enums.WorkflowExecutionStatus;

import java.util.UUID;

public record WorkflowFinished(UUID workflowRunId, WorkflowExecutionStatus status) {
}
