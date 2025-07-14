package org.phong.zenflow.workflow.subdomain.runner.dto;

import java.util.UUID;

public record WorkflowRunnerResponse(UUID workflowRunId, String statusUrl) {
}
