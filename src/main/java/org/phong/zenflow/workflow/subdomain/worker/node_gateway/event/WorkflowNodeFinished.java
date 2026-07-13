package org.phong.zenflow.workflow.subdomain.worker.node_gateway.event;

import lombok.Getter;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.dtos.BaseTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.enums.WorkflowExecutionStatus;

import java.util.UUID;

@Getter
public class WorkflowNodeFinished extends BaseTaskEnvelope {
    private final ExecutionResult executionResult;
    private final String message;

    public WorkflowNodeFinished(UUID workflowRunId,
                                WorkflowExecutionStatus status,
                                ExecutionResult result,
                                String message) {
        super(workflowRunId);
        this.status = status;
        this.executionResult = result;
        this.message = message;
    }

    private final WorkflowExecutionStatus status;
}
