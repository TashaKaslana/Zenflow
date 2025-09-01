package org.phong.zenflow.workflow.subdomain.runner.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.util.Map;

public record WorkflowRunnerRequest(
        @NotNull String callbackUrl,
        String startFromNodeKey,
        @Nullable Map<String, Object> payload
) {
    public WorkflowRunnerRequest(String callbackUrl, String startFromNodeKey) {
        this(callbackUrl, startFromNodeKey, null);
    }
}