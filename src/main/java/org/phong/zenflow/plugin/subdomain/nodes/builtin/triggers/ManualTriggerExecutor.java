package org.phong.zenflow.plugin.subdomain.nodes.builtin.triggers;

import lombok.extern.slf4j.Slf4j;
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
public class ManualTriggerExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core:manual.trigger:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logs = new LogCollector();
        try {
            log.info("Executing ManualTriggerExecutor with config: {}", config);

            logs.info("Manual trigger started at {}", OffsetDateTime.now());

            // Extract optional payload from input
            Map<String, Object> input = config.input();
            Object payload = input.get("payload");

            // Create output map with trigger metadata and payload
            Map<String, Object> output = new HashMap<>();
            output.put("trigger_type", "manual");
            output.put("triggered_at", OffsetDateTime.now().toString());
            output.put("trigger_source", "manual_execution");

            // Include payload in output if provided
            if (payload != null) {
                output.put("payload", payload);
                logs.info("Payload received: {}", payload);
            } else {
                logs.info("No payload provided");
            }

            // Add any additional input parameters to output for flexibility
            input.forEach((key, value) -> {
                if (!"payload".equals(key)) { // Avoid duplication
                    output.put("input_" + key, value);
                }
            });

            logs.success("Manual trigger completed successfully");

            return ExecutionResult.success(output, logs.getLogs());
        } catch (Exception e) {
            logs.error("Unexpected error occurred during manual trigger execution: {}", e.getMessage());
            log.error("Unexpected error during manual trigger execution", e);
            return ExecutionResult.error(e.getMessage(), logs.getLogs());
        }
    }
}