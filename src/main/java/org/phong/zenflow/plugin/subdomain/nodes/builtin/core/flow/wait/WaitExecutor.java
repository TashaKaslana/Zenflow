package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.wait;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.util.Map;

@Component
@PluginNode(
        key = "core:wait",
        name = "Wait",
        version = "1.0.0",
        description = "Waits for specified nodes to reach a certain state before proceeding.",
        type = "flow.wait",
        tags = {"core", "flow", "wait"},
        icon = "wait"
)
@Slf4j
@AllArgsConstructor
public class WaitExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core:wait:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher log = context.getLogPublisher();
        try {
            log.info("Starting wait node execution");
            String mode = (String) config.input().getOrDefault("mode", "any");
            int threshold = 1;
            if (mode.equals("threshold")) {
                threshold = ((Number) config.input().get("threshold")).intValue();
            }
            Map<String, Boolean> waitingNodes = ObjectConversion.convertObjectToMap(config.input().get("waitingNodes"), Boolean.class);

            if (waitingNodes == null || waitingNodes.isEmpty()) {
                log.error("No waiting nodes provided in the input.");
                return ExecutionResult.error("No waiting nodes provided");
            }

            boolean isReady = isReady(waitingNodes, mode, threshold);
            log.info("Status of waiting nodes: {}", waitingNodes);
            Map<String, Object> output = Map.of(
                "waitingNodes", waitingNodes,
                "mode", mode,
                "threshold", threshold,
                "isReady", isReady
            );

            if (!isReady) {
                Long timeoutMs = extractTimeout(config);
                ExecutionStatus fallbackStatus = extractFallbackStatus(config);

                if (timeoutMs != null) {
                    String timerKey = buildTimerKey(config);
                    Long start = context.read(timerKey, Long.class);
                    if (start == null) {
                        start = System.currentTimeMillis();
                        context.write(timerKey, start);
                    }

                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed >= timeoutMs) {
                        String message = String.format("Timeout after %dms", timeoutMs);
                        if (fallbackStatus != null && fallbackStatus != ExecutionStatus.ERROR) {
                            return buildFallbackResult(fallbackStatus, output, message);
                        }
                        return ExecutionResult.error(message);
                    }
                }

                return ExecutionResult.uncommit(output);
            }

            return ExecutionResult.commit(output);
        } catch (Exception e) {
            log.error("Failed to process wait node", e);
            log.withException(e).error("Failed to process wait node: " + e.getMessage());
            return ExecutionResult.error("Failed to process wait node: " + e.getMessage());
        }
    }

    public boolean isReady(Map<String, Boolean> waitingNodes, String mode, int threshold) {
        long committed = waitingNodes.values()
                .stream()
                .filter(Boolean::booleanValue)
                .count();

        return switch (mode) {
            case "all" -> committed == waitingNodes.size();
            case "any" -> committed >= 1;
            case "threshold" -> committed >= threshold;
            default -> false;
        };
    }

    private Long extractTimeout(WorkflowConfig config) {
        Object timeoutObj = config.input().get("timeoutMs");
        if (timeoutObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private ExecutionStatus extractFallbackStatus(WorkflowConfig config) {
        Object fallbackObj = config.input().get("fallbackStatus");
        if (fallbackObj == null) {
            return null;
        }
        return ExecutionStatus.fromString(fallbackObj.toString());
    }

    private String buildTimerKey(WorkflowConfig config) {
        return "wait:start:" + config.input().hashCode();
    }

    private ExecutionResult buildFallbackResult(ExecutionStatus status,
                                                Map<String, Object> output,
                                                String message) {
        return switch (status) {
            case COMMIT -> ExecutionResult.commit(output);
            case UNCOMMIT -> ExecutionResult.uncommit(output);
            case WAITING -> ExecutionResult.waiting();
            case SUCCESS -> ExecutionResult.success(output);
            default -> ExecutionResult.error(message);
        };
    }
}
