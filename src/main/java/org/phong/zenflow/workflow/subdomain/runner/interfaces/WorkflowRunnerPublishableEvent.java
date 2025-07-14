package org.phong.zenflow.workflow.subdomain.runner.interfaces;

import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.workflow_run.enums.TriggerType;

import java.util.UUID;

public interface WorkflowRunnerPublishableEvent {
    UUID getWorkflowRunId();

    TriggerType getTriggerType();

    UUID getWorkflowId();

    WorkflowRunnerRequest getRequest();
}
