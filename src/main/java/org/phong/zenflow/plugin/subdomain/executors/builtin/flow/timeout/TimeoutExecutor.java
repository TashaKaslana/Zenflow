package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.timeout;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimeoutExecutor implements PluginNodeExecutor {

    private final TimeoutScheduler timeoutScheduler;

    @Override
    public String key() {
        return "core:flow.timeout:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config) {
        LogCollector logCollector = new LogCollector();
        logCollector.info("Starting timeout node execution");

        Map<String, Object> input = config.input();
        String duration = (String) input.get("duration");
        String unit = (String) input.get("unit");

        if (duration == null || unit == null) {
            logCollector.error("Timeout duration and unit must be specified.");
            throw new IllegalArgumentException("Timeout duration and unit must be specified.");
        }

        long millis = parseDuration(duration, unit);

        UUID workflowId = UUID.fromString(input.get("workflowId").toString());
        UUID workflowRunId = UUID.fromString(input.get("workflowRunId").toString());
        String nodeKey = (String) input.get("nodeKey");

        timeoutScheduler.scheduleTimeout(workflowId, workflowRunId, nodeKey, millis);
        logCollector.info("Timeout scheduled for {} {} ({} milliseconds)", duration, unit, millis);

        return ExecutionResult.waiting(logCollector.getLogs());
    }

    private long parseDuration(String duration, String unit) {
        long value = Long.parseLong(duration);
        if (value < 0) {
            throw new IllegalArgumentException("Timeout duration must be a non-negative number.");
        }
        return switch (unit.toLowerCase()) {
            case "milliseconds" -> value;
            case "seconds" -> value * 1000;
            default -> throw new IllegalArgumentException("Only 'milliseconds' and 'seconds' are supported.");
        };
    }
}