package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.wait;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class WaitExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher log = context.getLogPublisher();
        log.info("Starting wait node execution");

        String mode = context.read("mode", String.class);
        if (mode == null) {
            mode = "any";
        }
        Integer thresholdAsInteger = context.read("threshold", Integer.class);
        int threshold = thresholdAsInteger != null ? thresholdAsInteger : 1;
        if (mode.equals("threshold")) {
            if (thresholdAsInteger == null) {
                // threshold is required for threshold mode, but let's default to 1 to avoid NPE
                threshold = 1;
            }
        }

        Object waitingNodesObj = context.read("waitingNodes", Object.class);
        Map<String, Boolean> waitingNodes = ObjectConversion.safeConvert(waitingNodesObj, new TypeReference<Map<String, Boolean>>() {});

        if (waitingNodes == null || waitingNodes.isEmpty()) {
            log.error("No waiting nodes provided in the input.");
            return ExecutionResult.error("No waiting nodes provided");
        }

        boolean isReady = isReady(waitingNodes, mode, threshold);
        log.info("Status of waiting nodes: {}", waitingNodes);
        Map<String, Object> output = new HashMap<>();
        output.put("waitingNodes", waitingNodes);
        output.put("mode", mode);
        output.put("threshold", threshold);
        output.put("isReady", isReady);


        if (!isReady) {
            Long timeoutMs = context.read("timeoutMs", Long.class);
            String fallbackStatusStr = context.read("fallbackStatus", String.class);
            ExecutionStatus fallbackStatus = fallbackStatusStr != null ? ExecutionStatus.fromString(fallbackStatusStr) : null;

            if (timeoutMs != null) {
                String timerKey = "wait:start:" + context.getNodeKey(); // build a timer key based on node key
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
