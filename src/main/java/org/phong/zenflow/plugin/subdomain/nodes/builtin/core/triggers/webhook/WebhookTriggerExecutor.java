package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.webhook;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.trigger.dto.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContextTool;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.plugin.subdomain.resource.NodeResourcePool;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;

@Component
@Slf4j
@AllArgsConstructor
public class WebhookTriggerExecutor implements TriggerExecutor {
    @Override
    public Optional<NodeResourcePool<?, ?>> getResourceManager() {
        return Optional.empty(); // Webhook triggers don't need resource pooling
    }

    @Override
    public Optional<String> getResourceKey(TriggerContext triggerCtx) {
        return Optional.empty(); // No resource key needed
    }

    @Override
    public RunningHandle start(TriggerContext triggerCtx, TriggerContextTool contextTool) throws Exception {
        WorkflowTrigger trigger = triggerCtx.trigger();
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
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("Executing WebhookTriggerExecutor with config: {}", context.getCurrentConfig());
        logs.info("Webhook trigger started at {}", OffsetDateTime.now());

        Object payload = context.read("payload", Object.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> headers = context.read("headers", Map.class);
        String httpMethod = context.read("http_method", String.class);
        String sourceIp = context.read("source_ip", String.class);
        String userAgent = context.read("user_agent", String.class);
        String webhookId = context.read("webhook_id", String.class);

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

        logs.success("Webhook trigger completed successfully");
        return ExecutionResult.success(output);
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