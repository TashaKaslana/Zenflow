package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.timeout;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimeoutExecutor implements NodeExecutor {

    private final TimeoutScheduler timeoutScheduler;
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        logCollector.info("Starting timeout node execution");

        String duration = context.read("duration", String.class);
        String unit = context.read("unit", String.class);

        if (duration == null || unit == null) {
            logCollector.error("Timeout duration and unit must be specified.");
            throw new IllegalArgumentException("Timeout duration and unit must be specified.");
        }

        long millis = parseDuration(duration, unit);

        UUID workflowId = context.getWorkflowId();
        UUID workflowRunId = context.getWorkflowRunId();
        String nodeKey = context.getNodeKey();

        timeoutScheduler.scheduleTimeout(workflowId, workflowRunId, nodeKey, millis);
        logCollector.info("Timeout scheduled for {} {} ({} milliseconds)", duration, unit, millis);

        return ExecutionResult.waiting();
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