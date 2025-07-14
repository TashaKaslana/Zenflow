package org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.timeout.executor;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.timeout.TimeoutDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;

//TODO: a draft implementation of TimeoutExecutor, consider change status to 'paused' and add time to wakeup instead of sleeping the thread
@Component
public class TimeoutExecutor implements NodeExecutor<TimeoutDefinition> {

    private static final long MAX_SLEEP_MILLIS = 30_000;

    @Override
    public String getNodeType() {
        return "timeout";
    }

    @Override
    public ExecutionResult execute(TimeoutDefinition node, Map<String, Object> context) {
        try {
            if (node.getDuration() == null || node.getUnit() == null) {
                throw new IllegalArgumentException("Timeout duration and unit must be specified.");
            }

            long millis = parseDuration(node.getDuration(), node.getUnit());
            Thread.sleep(millis);

            return ExecutionResult.nextNode(node.getNext().isEmpty() ? null : node.getNext().getFirst());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Timeout interrupted.", e);
        } catch (Exception e) {
            throw new RuntimeException("Invalid timeout configuration: " + e.getMessage(), e);
        }
    }

    private long parseDuration(String duration, String unit) {
        long value = Long.parseLong(duration);

        long millis = switch (unit.toLowerCase()) {
            case "milliseconds" -> value;
            case "seconds" -> value * 1000;
            default -> throw new IllegalArgumentException("Only 'milliseconds' and 'seconds' are allowed.");
        };

        if (millis > MAX_SLEEP_MILLIS) {
            throw new IllegalArgumentException("Timeout duration exceeds limit of " + MAX_SLEEP_MILLIS + "ms.");
        }

        return millis;
    }
}

