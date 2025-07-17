package org.phong.zenflow.workflow.subdomain.runner.dto;

import jakarta.validation.constraints.NotNull;

public record WorkflowRunnerRequest(
        @NotNull String callbackUrl,
        String startFromNodeKey
) {
}
