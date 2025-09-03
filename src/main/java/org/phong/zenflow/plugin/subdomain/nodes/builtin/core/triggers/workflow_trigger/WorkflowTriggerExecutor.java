package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.workflow_trigger;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.resource.TriggerResourceManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.time.OffsetDateTime;
import java.util.*;

@Component
@PluginNode(
        key = "core:workflow.trigger",
        name = "Workflow Trigger",
        version = "1.0.0",
        type = "trigger",
        triggerType = "manual",
        description = "Triggers a workflow execution based on provided parameters.",
        tags = {"workflow", "trigger", "execution"},
        icon = "ph:rocket-launch"
)
@AllArgsConstructor
@Slf4j
public class WorkflowTriggerExecutor implements TriggerExecutor {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String key() {
        return "core:workflow.trigger:1.0.0";
    }

    @Override
    public Optional<TriggerResourceManager<?>> getResourceManager() {
        return Optional.empty(); // Workflow triggers don't need resource pooling
    }

    @Override
    public Optional<String> getResourceKey(WorkflowTrigger trigger) {
        return Optional.empty(); // No resource key needed
    }

    @Override
    public RunningHandle start(WorkflowTrigger trigger, TriggerContext ctx) throws Exception {
        log.info("Starting workflow trigger for workflow: {}", trigger.getWorkflowId());

        Map<String, Object> config = trigger.getConfig();
        UUID targetWorkflowId = UUID.fromString(config.get("target_workflow_id").toString());
        Boolean isAsync = (Boolean) config.getOrDefault("is_async", false);

        log.info("Workflow trigger registered for workflow: {} to trigger target workflow: {}",
                trigger.getWorkflowId(), targetWorkflowId);

        return new WorkflowRunningHandle(trigger.getId(), targetWorkflowId, isAsync);
    }

    @Override
    @Transactional
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        try {
            logs.info("Workflow trigger started at {}", OffsetDateTime.now());

            Map<String, Object> input = config.input();

            //TODO: ensure trigger workflow is enabled, exists and is owned by the user
            UUID workflowRunId = UUID.fromString(input.get("workflow_run_id").toString());
            UUID workflowId = UUID.fromString(input.get("workflow_id").toString());
            boolean isAsync = (boolean) input.getOrDefault("is_async", false);
            String startFromNodeKey = (String) input.get("start_from_node_key");
            String callbackUrl = (String) input.get("callback_url");
            Object payload = input.get("payload");

            logs.info("Triggering workflow with ID: {} and run ID: {}", workflowId, workflowRunId);

            WorkflowRunnerRequest runnerRequest;
            if (startFromNodeKey != null) {
                runnerRequest = new WorkflowRunnerRequest(
                    callbackUrl,
                    startFromNodeKey
                );

                if (payload != null) {
                    logs.info("Payload will be available in workflow context: {}", payload);
                }
            } else {
                log.warn("No start_from_node_key provided, can't start the workflow.");
                return ExecutionResult.error("'start_from_node_key' is required to trigger the workflow.");
            }

            eventPublisher.publishEvent(new WorkflowTriggerEvent(
                    workflowRunId,
                    isAsync ? TriggerType.EVENT : TriggerType.MANUAL,
                    workflowId,
                    runnerRequest
            ));

            Map<String, Object> output = new HashMap<>();
            output.put("trigger_type", "workflow");
            output.put("triggered_at", OffsetDateTime.now().toString());
            output.put("trigger_source", "workflow_trigger");
            output.put("target_workflow_id", workflowId.toString());
            output.put("workflow_run_id", workflowRunId.toString());
            output.put("is_async", isAsync);

            output.put("start_from_node_key", startFromNodeKey);

            if (callbackUrl != null) {
                output.put("callback_url", callbackUrl);
            }

            if (payload != null) {
                output.put("payload", payload);
                logs.info("Payload included: {}", payload);
            }

            logs.success("Workflow trigger event published successfully");
            return ExecutionResult.success(output);
        } catch (Exception e) {
            logs.withException(e).error("Unexpected error occurred during workflow trigger execution: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }

    /**
     * Running handle for workflow triggers
     */
    private static class WorkflowRunningHandle implements RunningHandle {
        private final UUID triggerId;
        private final UUID targetWorkflowId;
        private final Boolean isAsync;
        private volatile boolean running = true;

        public WorkflowRunningHandle(UUID triggerId, UUID targetWorkflowId, Boolean isAsync) {
            this.triggerId = triggerId;
            this.targetWorkflowId = targetWorkflowId;
            this.isAsync = isAsync;
            // startFromNodeKey and callbackUrl are configuration details stored for future use
        }

        @Override
        public void stop() {
            if (running) {
                running = false;
                log.info("Workflow trigger stopped: {} (target: {}, async: {})",
                        triggerId, targetWorkflowId, isAsync);
            }
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public String getStatus() {
            return running ? String.format("READY (target: %s, async: %s)", targetWorkflowId, isAsync) : "STOPPED";
        }
    }
}