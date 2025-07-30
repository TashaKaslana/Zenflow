package org.phong.zenflow.plugin.subdomain.executors.builtin.triggers;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
@AllArgsConstructor
public class WorkflowTriggerExecutor implements PluginNodeExecutor {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String key() {
        return "core:workflow.trigger:1.0.0";
    }

    @Override
    @Transactional
    public ExecutionResult execute(WorkflowConfig config) {
        LogCollector logs = new LogCollector();
        logs.info("Executing TriggerWorkflowExecutor with config: " + config);

        // Extract the 'input' map from the config
        Map<String, Object> input = config.input();

        //TODO: ensure trigger workflow is enabled, exists and is own by the user
        UUID workflowRunId = UUID.fromString(input.get("workflow_run_id").toString());
        UUID workflowId = UUID.fromString(input.get("workflow_id").toString());
        boolean isAsync = (boolean) input.getOrDefault("is_async", false);

        logs.info("Triggering workflow with ID: " + workflowId + " and run ID: " + workflowRunId);

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
                return null;
            }
        });

        logs.success("Create workflow trigger request successfully with ID: " + workflowId + " and run ID: " + workflowRunId);
        return ExecutionResult.success(null, logs.getLogs());
    }
}