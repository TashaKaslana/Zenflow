package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.workflow_trigger;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContextTool;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.plugin.subdomain.resource.NodeResourcePool;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Component
@AllArgsConstructor
@Slf4j
public class WorkflowTriggerExecutor implements TriggerExecutor {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Optional<NodeResourcePool<?, ?>> getResourceManager() {
        return Optional.empty(); // Workflow triggers don't need resource pooling
    }

    @Override
    public Optional<String> getResourceKey(TriggerContext triggerCtx) {
        return Optional.empty(); // No resource key needed
    }

    @Override
    public RunningHandle start(TriggerContext triggerCtx, TriggerContextTool contextTool) throws Exception {
        WorkflowTrigger trigger = triggerCtx.trigger();
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
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("Workflow trigger started at {}", OffsetDateTime.now());

        UUID workflowRunId = context.read("workflow_run_id", UUID.class);
        UUID workflowId = context.read("workflow_id", UUID.class);
        boolean isAsync = context.read("is_async", Boolean.class);
        String startFromNodeKey = context.read("start_from_node_key", String.class);
        String callbackUrl = context.read("callback_url", String.class);
        Object payload = context.read("payload", Object.class);

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

        context.write("trigger_type", "workflow");
        context.write("triggered_at", OffsetDateTime.now().toString());
        context.write("trigger_source", "workflow_trigger");
        context.write("target_workflow_id", workflowId.toString());
        context.write("workflow_run_id", workflowRunId.toString());
        context.write("is_async", isAsync);

        context.write("start_from_node_key", startFromNodeKey);

        if (callbackUrl != null) {
            context.write("callback_url", callbackUrl);
        }

        if (payload != null) {
            context.write("payload", payload);
            logs.info("Payload included: {}", payload);
        }

        logs.success("Workflow trigger event published successfully");
        return ExecutionResult.success();
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