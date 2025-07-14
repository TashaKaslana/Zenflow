package org.phong.zenflow.workflow.subdomain.runner.event;

import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;

import java.util.UUID;

public interface WorkflowRunnerPublishableEvent {
    UUID getWorkflowRunId();

    TriggerType getTriggerType();

    UUID getWorkflowId();

    WorkflowRunnerRequest request();
}
