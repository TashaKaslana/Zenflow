package org.phong.zenflow.workflow.subdomain.trigger.dto;

import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;

import java.util.UUID;

public record WorkflowTriggerEvent(UUID workflowRunId,
                                   TriggerType triggerType,
                                   UUID triggerExecutorId,
                                   UUID workflowId,
                                   WorkflowRunnerRequest request) implements WorkflowRunnerPublishableEvent {
    @Override
    public UUID getWorkflowRunId() {
        return workflowRunId;
    }

    @Override
    public TriggerType getTriggerType() {
        return triggerType;
    }

    @Override
    public UUID getTriggerExecutorId() {
        return triggerExecutorId;
    }

    @Override
    public UUID getWorkflowId() {
        return workflowId;
    }

    @Override
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
