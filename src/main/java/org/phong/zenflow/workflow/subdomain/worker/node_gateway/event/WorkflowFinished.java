package org.phong.zenflow.workflow.subdomain.worker.node_gateway.event;

import lombok.Getter;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.dtos.BaseTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.enums.WorkflowExecutionStatus;

import java.util.UUID;

@Getter
public final class WorkflowFinished extends BaseTaskEnvelope {
    private final UUID workflowRunId;
    private final WorkflowExecutionStatus status;
    private final String message;

    public WorkflowFinished(UUID workflowRunId, WorkflowExecutionStatus status, String message) {
        super(workflowRunId);
        this.workflowRunId = workflowRunId;
        this.status = status;
        this.message = message;
    }
}
