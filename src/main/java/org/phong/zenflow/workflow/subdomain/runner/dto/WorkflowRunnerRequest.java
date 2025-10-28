package org.phong.zenflow.workflow.subdomain.runner.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * Request to run a workflow.
 * 
 * @param callbackUrl URL to notify when workflow completes
 * @param startFromNodeKey Optional node key to start execution from (for trigger nodes)
 * @param payload Optional initial data to inject into workflow context
 * @param payloadMetadata Optional metadata hints for payload values (e.g., base64 encoding)
 */
public record WorkflowRunnerRequest(
        @NotNull String callbackUrl,
        String startFromNodeKey,
        @Nullable Map<String, Object> payload,
        @Nullable Map<String, PayloadMetadata> payloadMetadata
) {
    public WorkflowRunnerRequest(String callbackUrl, String startFromNodeKey) {
        this(callbackUrl, startFromNodeKey, null, null);
    }
    
    public WorkflowRunnerRequest(String callbackUrl, String startFromNodeKey, Map<String, Object> payload) {
        this(callbackUrl, startFromNodeKey, payload, null);
    }
}