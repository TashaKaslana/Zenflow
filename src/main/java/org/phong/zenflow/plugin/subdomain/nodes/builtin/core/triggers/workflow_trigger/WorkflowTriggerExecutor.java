package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.workflow_trigger;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
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
        description = "Triggers a workflow execution based on provided parameters.",
        tags = {"workflow", "trigger", "execution"},
        icon = "ph:rocket-launch"
)
@AllArgsConstructor
@Slf4j
public class WorkflowTriggerExecutor implements PluginNodeExecutor {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String key() {
        return "core:workflow.trigger:1.0.0";
    }

    @Override
    @Transactional
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        LogCollector logs = new LogCollector();
        try {
            logs.info("Workflow trigger started at {}", OffsetDateTime.now());

            // Extract the 'input' map from the config
            Map<String, Object> input = config.input();

            //TODO: ensure trigger workflow is enabled, exists and is owned by the user
            UUID workflowRunId = UUID.fromString(input.get("workflow_run_id").toString());
            UUID workflowId = UUID.fromString(input.get("workflow_id").toString());
            boolean isAsync = (boolean) input.getOrDefault("is_async", false);
            String startFromNodeKey = (String) input.get("start_from_node_key");
            String callbackUrl = (String) input.get("callback_url");
            Object payload = input.get("payload");

            logs.info("Triggering workflow with ID: {} and run ID: {}", workflowId, workflowRunId);

            // Create workflow runner request with available parameters
            WorkflowRunnerRequest runnerRequest = null;
            if (startFromNodeKey != null || callbackUrl != null) {
                runnerRequest = new WorkflowRunnerRequest(
                    callbackUrl,
                    startFromNodeKey
                );

                if (payload != null) {
                    logs.info("Payload will be available in workflow context: {}", payload);
                }
            }

            final WorkflowRunnerRequest finalRequest = runnerRequest;
            eventPublisher.publishEvent(new WorkflowRunnerPublishableEvent() {
                @Override
                public UUID getWorkflowRunId() {
                    return workflowRunId;
                }

                @Override
                public TriggerType getTriggerType() {
                    return isAsync ? TriggerType.EVENT : TriggerType.MANUAL;
                }

                @Override
                public UUID getWorkflowId() {
                    return workflowId;
                }

                @Override
                public WorkflowRunnerRequest request() {
                    return finalRequest;
                }
            });

            // Create output with trigger metadata and payload information
            Map<String, Object> output = new HashMap<>();
            output.put("trigger_type", "workflow");
            output.put("triggered_at", OffsetDateTime.now().toString());
            output.put("trigger_source", "workflow_execution");
            output.put("target_workflow_id", workflowId.toString());
            output.put("target_workflow_run_id", workflowRunId.toString());
            output.put("is_async", isAsync);

            if (startFromNodeKey != null) {
                output.put("start_from_node_key", startFromNodeKey);
            }

            if (callbackUrl != null) {
                output.put("callback_url", callbackUrl);
            }

            if (payload != null) {
                output.put("forwarded_payload", payload);
            }

            // Add any additional input parameters to output for flexibility
            Set<String> excludedKeys = Set.of("workflow_run_id", "workflow_id", "is_async", "start_from_node_key", "callback_url", "payload");
            input.forEach((key, value) -> {
                if (!excludedKeys.contains(key)) {
                    output.put("input_" + key, value);
                }
            });

            logs.success("Workflow trigger request created successfully for workflow ID: {} and run ID: {}", workflowId, workflowRunId);

            return ExecutionResult.success(output, logs.getLogs());
        } catch (IllegalArgumentException e) {
            logs.error("Invalid input parameter: {}", e.getMessage());
            log.error("Invalid input parameter for workflow trigger", e);
            return ExecutionResult.error("Invalid input: " + e.getMessage(), logs.getLogs());
        } catch (Exception e) {
            logs.error("Unexpected error occurred during workflow trigger execution: {}", e.getMessage());
            log.error("Unexpected error during workflow trigger execution", e);
            return ExecutionResult.error(e.getMessage(), logs.getLogs());
        }
    }
}