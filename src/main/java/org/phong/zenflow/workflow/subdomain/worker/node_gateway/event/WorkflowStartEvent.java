package org.phong.zenflow.workflow.subdomain.worker.node_gateway.event;

import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;

import java.util.UUID;

public record WorkflowStartEvent(
        UUID workflowRunId,
        TriggerType triggerType,
        UUID triggerExecutorId,
        UUID workflowId,
        WorkflowRunnerRequest request
)  {
    public UUID getWorkflowRunId() {
        return workflowRunId;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public UUID getTriggerExecutorId() {
        return triggerExecutorId;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public WorkflowRunnerRequest request() {
        return request;
    }

    public WorkflowStartEvent(UUID workflowRunId,
                              TriggerType triggerType,
                              UUID workflowId,
                              WorkflowRunnerRequest request) {
        this(workflowRunId, triggerType, null, workflowId, request);
    }
}
