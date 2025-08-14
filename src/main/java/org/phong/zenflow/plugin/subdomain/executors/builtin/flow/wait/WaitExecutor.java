package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.wait;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionInput;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextPool;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.LogEntry;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class WaitExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core:wait:1.0.0";
    }

    @Override
    public ExecutionResult execute(ExecutionInput executionInput) {
        LogCollector logCollector = new LogCollector();
        WorkflowConfig config = executionInput.config();
        RuntimeContext context = RuntimeContextPool.getContext(executionInput.metadata().workflowRunId());
        try {
            logCollector.info("Starting wait node execution");
            String mode = (String) config.input().getOrDefault("mode", "any");
            int threshold = 1;
            if (mode.equals("threshold")) {
                threshold = ((Number) config.input().get("threshold")).intValue();
            }
            Map<String, Boolean> waitingNodes = ObjectConversion.convertObjectToMap(config.input().get("waitingNodes"), Boolean.class);

            if (waitingNodes == null || waitingNodes.isEmpty()) {
                logCollector.error("No waiting nodes provided in the input.");
                return ExecutionResult.error("No waiting nodes provided", logCollector.getLogs());
            }

            boolean isReady = isReady(waitingNodes, mode, threshold);
            logCollector.info("Status of waiting nodes: {}", waitingNodes);
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
                    Long start = (Long) context.get(timerKey);
                    if (start == null) {
                        start = System.currentTimeMillis();
                        context.put(timerKey, start);
                    }

                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed >= timeoutMs) {
                        String message = String.format("Timeout after %dms", timeoutMs);
                        if (fallbackStatus != null && fallbackStatus != ExecutionStatus.ERROR) {
                            return buildFallbackResult(fallbackStatus, output, logCollector.getLogs(), message);
                        }
                        return ExecutionResult.error(message, logCollector.getLogs());
                    }
                }

                return ExecutionResult.uncommit(output, logCollector.getLogs());
            }

            return ExecutionResult.commit(output, logCollector.getLogs());
        } catch (Exception e) {
            log.error("Failed to process wait node", e);
            logCollector.error("Failed to process wait node: " + e.getMessage());
            return ExecutionResult.error("Failed to process wait node: " + e.getMessage(), logCollector.getLogs());
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
                                                List<LogEntry> logs,
                                                String message) {
        return switch (status) {
            case COMMIT -> ExecutionResult.commit(output, logs);
            case UNCOMMIT -> ExecutionResult.uncommit(output, logs);
            case WAITING -> ExecutionResult.waiting(logs);
            case SUCCESS -> ExecutionResult.success(output, logs);
            default -> ExecutionResult.error(message, logs);
        };
    }
}
