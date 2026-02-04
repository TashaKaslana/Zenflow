package org.phong.zenflow.workflow.subdomain.worker.node_gateway.event;

import lombok.Getter;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.dtos.BaseTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.enums.WorkflowExecutionStatus;

import java.util.UUID;

@Getter
public class WorkflowNodeFinished extends BaseTaskEnvelope {
    public WorkflowNodeFinished(UUID workflowRunId, WorkflowExecutionStatus status) {
        super(workflowRunId);
        this.status = status;
    }

    private final WorkflowExecutionStatus status;
}
