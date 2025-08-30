package org.phong.zenflow.workflow.dto;

import java.util.UUID;

public record ExecuteWorkflowResponse(UUID workflowRunId, String statusUrl) {

    public static ExecuteWorkflowResponse of(UUID workflowRunId) {
        return new ExecuteWorkflowResponse(workflowRunId, String.format("/workflow-runs/%s", workflowRunId));
    }
}
