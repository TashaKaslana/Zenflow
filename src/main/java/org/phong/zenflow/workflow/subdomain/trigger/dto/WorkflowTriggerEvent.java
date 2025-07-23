package org.phong.zenflow.workflow.subdomain.trigger.dto;

import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;

import java.util.UUID;

public record WorkflowTriggerEvent(UUID workflowRunId, WorkflowTrigger workflowTrigger,
                                   WorkflowRunnerRequest request) implements WorkflowRunnerPublishableEvent {
    @Override
    public UUID getWorkflowRunId() {
        return workflowRunId;
    }

    @Override
    public TriggerType getTriggerType() {
        return workflowTrigger.getType();
    }

    @Override
    public UUID getWorkflowId() {
        return workflowTrigger.getWorkflowId();
    }

    @Override
    public WorkflowRunnerRequest request() {
        return request;
    }
}
