package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.wait;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

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
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logCollector = new LogCollector();
        try {
            logCollector.info("Starting wait node execution");
            String mode = (String) config.input().getOrDefault("mode", "any");
            int threshold = 1;
            if (mode.equals("threshold")) {
                threshold = (int) config.input().get("threshold");
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
                return ExecutionResult.uncommit(output, logCollector.getLogs());
            }

            return ExecutionResult.commit(output, logCollector.getLogs());
        } catch (Exception e) {
            log.error("Failed to process wait node", e);
            logCollector.error("Failed to process wait node: " + e.getMessage());
            return ExecutionResult.error("Failed to process wait node: " + e.getMessage(), logCollector.getLogs());
        }
    }

    boolean isReady(Map<String, Boolean> waitingNodes, String mode, int threshold) {
        long committed = waitingNodes.values().stream().filter(Boolean::booleanValue).count();

        return switch (mode) {
            case "all" -> committed == waitingNodes.size();
            case "any" -> committed >= 1;
            case "threshold" -> committed >= threshold;
            default -> false;
        };
    }
}
