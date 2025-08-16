package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@Slf4j
@AllArgsConstructor
public class IfNodeExecutor implements PluginNodeExecutor {
    private static final Pattern UNRESOLVED_PATTERN = Pattern.compile("\\{\\{[^}]+}}");

    @Override
    public String key() {
        return "core:flow.branch.if:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logCollector = new LogCollector();
        try {
            Map<String, Object> input = config.input();
            String condition = (String) input.get("condition"); // e.g. "true", "1 > 0"

            List<String> nextTrue = ObjectConversion.safeConvert(input.get("next_true"), new TypeReference<>() {
            });
            List<String> nextFalse = ObjectConversion.safeConvert(input.get("next_false"), new TypeReference<>() {
            });

            logCollector.info("Begin if flow with condition: {}", condition);

            if (condition == null || condition.isBlank()) {
                String errorMsg = "If condition is null or blank.";
                logCollector.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            if (UNRESOLVED_PATTERN.matcher(condition).find()) {
                logCollector.warning("Skipping condition due to unresolved placeholder: " + condition);
                return ExecutionResult.nextNode(getFirstOrNull(nextFalse), logCollector.getLogs());
            }

            log.debug("Evaluating IF condition: {}", condition);

            return getExpressionExecutionResult(condition, nextTrue, logCollector, nextFalse);
        } catch (Exception e) {
            log.error("Failed to process if-node", e);
            logCollector.error("Failed to process if-node: " + e.getMessage());
            return ExecutionResult.error("Failed to process if-node: " + e.getMessage(), logCollector.getLogs());
        }
    }

    private ExecutionResult getExpressionExecutionResult(String condition, List<String> nextTrue, LogCollector logCollector, List<String> nextFalse) {
        try {
            Object result = AviatorEvaluator.execute(condition);
            Boolean isMatch = (Boolean) result;

            String next;
            if (Boolean.TRUE.equals(isMatch)) {
                next = getFirstOrNull(nextTrue);
                logCollector.info("Condition matched: {} - proceeding to true branch: {}", condition, next);
            } else {
                next = getFirstOrNull(nextFalse);
                logCollector.info("Condition not matched: {} - proceeding to false branch: {}", condition, next);
            }
            return ExecutionResult.nextNode(next, logCollector.getLogs());
        } catch (Exception conditionException) {
            log.warn("Condition evaluation failed: {} - {}", condition, conditionException.getMessage());
            logCollector.warning("Condition evaluation failed: " + conditionException.getMessage());

            String next = getFirstOrNull(nextFalse);
            logCollector.info("Falling back to false branch due to evaluation error: {}", next);

            return ExecutionResult.nextNode(next, logCollector.getLogs());
        }
    }

    private String getFirstOrNull(List<String> list) {
        return (list != null && !list.isEmpty()) ? list.getFirst() : null;
    }
}