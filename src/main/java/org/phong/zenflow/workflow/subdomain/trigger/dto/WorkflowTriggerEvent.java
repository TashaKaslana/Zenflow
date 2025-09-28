package org.phong.zenflow.workflow.subdomain.trigger.dto;

import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;

import java.util.UUID;

public record WorkflowTriggerEvent(
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

    public WorkflowTriggerEvent(UUID workflowRunId,
                                TriggerType triggerType,
                                UUID workflowId,
                                WorkflowRunnerRequest request) {
        this(workflowRunId, triggerType, null, workflowId, request);
    }
}
