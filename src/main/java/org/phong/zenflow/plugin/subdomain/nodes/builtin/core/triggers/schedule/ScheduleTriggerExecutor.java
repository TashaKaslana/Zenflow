package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.schedule;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.time.OffsetDateTime;
import java.util.*;

@Component
@PluginNode(
        key = "core:schedule.trigger",
        name = "Schedule Trigger",
        version = "1.0.0",
        description = "Triggers a workflow based on a schedule defined by a cron expression. " +
                "This node can be used to initiate workflows at specified intervals or times.",
        type = "trigger",
        tags = {"core", "trigger", "schedule"},
        icon = "ph:clock"
)
@Slf4j
public class ScheduleTriggerExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core:schedule.trigger:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logs = new LogCollector();
        try {
            log.info("Executing ScheduleTriggerExecutor with config: {}", config);

            logs.info("Schedule trigger started at {}", OffsetDateTime.now());

            // Extract optional payload and schedule configuration from input
            Map<String, Object> input = config.input();
            Object payload = input.get("payload");
            String cronExpression = (String) input.get("cron_expression");
            String scheduleDescription = (String) input.get("schedule_description");

            // Create output map with trigger metadata and payload
            Map<String, Object> output = new HashMap<>();
            output.put("trigger_type", "schedule");
            output.put("triggered_at", OffsetDateTime.now().toString());
            output.put("trigger_source", "scheduled_execution");

            // Add schedule-specific metadata
            if (cronExpression != null) {
                output.put("cron_expression", cronExpression);
                logs.info("Schedule triggered with cron: {}", cronExpression);
            }

            if (scheduleDescription != null) {
                output.put("schedule_description", scheduleDescription);
            }

            // Include payload in output if provided
            if (payload != null) {
                output.put("payload", payload);
                logs.info("Payload received: {}", payload);
            } else {
                logs.info("No payload provided");
            }

            // Add any additional input parameters to output for flexibility
            input.forEach((key, value) -> {
                if (!Set.of("payload", "cron_expression", "schedule_description").contains(key)) {
                    output.put("input_" + key, value);
                }
            });

            logs.success("Schedule trigger completed successfully");

            return ExecutionResult.success(output, logs.getLogs());
        } catch (Exception e) {
            logs.error("Unexpected error occurred during schedule trigger execution: {}", e.getMessage());
            log.error("Unexpected error during schedule trigger execution", e);
            return ExecutionResult.error(e.getMessage(), logs.getLogs());
        }
    }
}