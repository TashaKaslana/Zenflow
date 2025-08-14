package org.phong.zenflow.plugin.subdomain.executors.builtin.triggers;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionInput;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextPool;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;

@Component
@Slf4j
public class WebhookTriggerExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core:webhook.trigger:1.0.0";
    }

    @Override
    public ExecutionResult execute(ExecutionInput executionInput) {
        WorkflowConfig config = executionInput.config();
        RuntimeContext context = RuntimeContextPool.getContext(executionInput.metadata().workflowRunId());
        LogCollector logs = new LogCollector();
        try {
            log.info("Executing WebhookTriggerExecutor with config: {}", config);

            logs.info("Webhook trigger started at {}", OffsetDateTime.now());

            // Extract webhook-specific data and payload from input
            Map<String, Object> input = config.input();
            Object payload = input.get("payload");
            Map<String, Object> headers = ObjectConversion.convertObjectToMap(input.get("headers"));
            String httpMethod = (String) input.get("http_method");
            String sourceIp = (String) input.get("source_ip");
            String userAgent = (String) input.get("user_agent");
            String webhookId = (String) input.get("webhook_id");

            // Create output map with trigger metadata and payload
            Map<String, Object> output = new HashMap<>();
            output.put("trigger_type", "webhook");
            output.put("triggered_at", OffsetDateTime.now().toString());
            output.put("trigger_source", "webhook_request");

            // Add webhook-specific metadata
            if (httpMethod != null) {
                output.put("http_method", httpMethod);
                logs.info("Webhook triggered via {} request", httpMethod);
            }

            if (sourceIp != null) {
                output.put("source_ip", sourceIp);
                logs.info("Request from IP: {}", sourceIp);
            }

            if (userAgent != null) {
                output.put("user_agent", userAgent);
            }

            if (webhookId != null) {
                output.put("webhook_id", webhookId);
            }

            if (headers != null && !headers.isEmpty()) {
                output.put("headers", headers);
                logs.info("Headers received: {} headers", headers.size());
            }

            // Include payload in output if provided
            if (payload != null) {
                output.put("payload", payload);
                logs.info("Webhook payload received: {}", payload);
            } else {
                logs.info("No payload provided in webhook request");
            }

            // Add any additional input parameters to output for flexibility
            Set<String> excludedKeys = Set.of("payload", "headers", "http_method", "source_ip", "user_agent", "webhook_id");
            input.forEach((key, value) -> {
                if (!excludedKeys.contains(key)) {
                    output.put("input_" + key, value);
                }
            });

            logs.success("Webhook trigger completed successfully");

            return ExecutionResult.success(output, logs.getLogs());
        } catch (Exception e) {
            logs.error("Unexpected error occurred during webhook trigger execution: {}", e.getMessage());
            log.error("Unexpected error during webhook trigger execution", e);
            return ExecutionResult.error(e.getMessage(), logs.getLogs());
        }
    }
}