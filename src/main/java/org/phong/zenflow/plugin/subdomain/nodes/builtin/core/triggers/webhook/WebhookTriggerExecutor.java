package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.webhook;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.plugin.subdomain.resource.NodeResourcePool;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.time.OffsetDateTime;
import java.util.*;

@Component
@PluginNode(
        key = "core:webhook.trigger",
        name = "Webhook Trigger",
        version = "1.0.0",
        description = "Executes when a webhook is triggered, processing the request data and metadata.",
        type = "trigger",
        triggerType = "webhook",
        tags = {"webhook", "trigger", "http", "api", "event" },
        icon = "ph:webhook"
)
@Slf4j
@AllArgsConstructor
public class WebhookTriggerExecutor implements TriggerExecutor {
    @Override
    public Optional<NodeResourcePool<?, ?>> getResourceManager() {
        return Optional.empty(); // Webhook triggers don't need resource pooling
    }

    @Override
    public Optional<String> getResourceKey(WorkflowTrigger trigger) {
        return Optional.empty(); // No resource key needed
    }

    @Override
    public RunningHandle start(WorkflowTrigger trigger, TriggerContext ctx) throws Exception {
        log.info("Starting webhook trigger for workflow: {}", trigger.getWorkflowId());

        Map<String, Object> config = trigger.getConfig();
        String webhookPath = (String) config.get("webhook_path");

        log.info("Webhook trigger registered for workflow: {} with path: {}",
                trigger.getWorkflowId(), webhookPath);

        // Note: The actual webhook endpoint registration is handled by the webhook service
        // This trigger just registers that it's ready to receive webhook events

        return new WebhookRunningHandle(trigger.getId(), webhookPath);
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        try {
            logs.info("Executing WebhookTriggerExecutor with config: {}", config);
            logs.info("Webhook trigger started at {}", OffsetDateTime.now());

            Map<String, Object> input = config.input();
            Object payload = input.get("payload");
            Map<String, Object> headers = ObjectConversion.convertObjectToMap(input.get("headers"));
            String httpMethod = (String) input.get("http_method");
            String sourceIp = (String) input.get("source_ip");
            String userAgent = (String) input.get("user_agent");
            String webhookId = (String) input.get("webhook_id");

            Map<String, Object> output = new HashMap<>();
            output.put("trigger_type", "webhook");
            output.put("triggered_at", OffsetDateTime.now().toString());
            output.put("trigger_source", "webhook_request");

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

            if (payload != null) {
                output.put("payload", payload);
                logs.info("Webhook payload received: {}", payload);
            } else {
                logs.info("No payload provided in webhook request");
            }

            Set<String> excludedKeys = Set.of("payload", "headers", "http_method", "source_ip", "user_agent", "webhook_id");
            input.forEach((key, value) -> {
                if (!excludedKeys.contains(key)) {
                    output.put("input_" + key, value);
                }
            });

            logs.success("Webhook trigger completed successfully");
            return ExecutionResult.success(output);
        } catch (Exception e) {
            logs.withException(e).error("Unexpected error occurred during webhook trigger execution: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }

    /**
     * Running handle for webhook triggers
     */
    private static class WebhookRunningHandle implements RunningHandle {
        private final UUID triggerId;
        private final String webhookPath;
        private volatile boolean running = true;

        public WebhookRunningHandle(UUID triggerId, String webhookPath) {
            this.triggerId = triggerId;
            this.webhookPath = webhookPath;
        }

        @Override
        public void stop() {
            if (running) {
                running = false;
                log.info("Webhook trigger stopped: {} (path: {})", triggerId, webhookPath);
                // Note: Actual webhook endpoint cleanup would be handled by webhook service
            }
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public String getStatus() {
            return running ? String.format("LISTENING (path: %s)", webhookPath) : "STOPPED";
        }
    }
}