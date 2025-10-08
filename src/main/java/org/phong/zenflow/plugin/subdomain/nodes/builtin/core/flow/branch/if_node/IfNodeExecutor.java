package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.if_node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class IfNodeExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
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

        log.debug("Evaluating IF condition: {}", condition);

        AviatorEvaluatorInstance evaluator = context.getEvaluator().cloneInstance();
        return getExpressionExecutionResult(condition, nextTrue, logCollector, nextFalse, context, evaluator);
    }

    private ExecutionResult getExpressionExecutionResult(String condition, List<String> nextTrue, NodeLogPublisher logCollector, List<String> nextFalse, ExecutionContext context, AviatorEvaluatorInstance evaluator) {
        try {
            Object result = evaluator.execute(condition, Map.of("context", context));
            Boolean isMatch = (Boolean) result;

            String next;
            if (Boolean.TRUE.equals(isMatch)) {
                next = getFirstOrNull(nextTrue);
                logCollector.info("Condition matched: {} - proceeding to true branch: {}", condition, next);
            } else {
                next = getFirstOrNull(nextFalse);
                logCollector.info("Condition not matched: {} - proceeding to false branch: {}", condition, next);
            }
            return ExecutionResult.nextNode(next);
        } catch (Exception conditionException) {
            log.warn("Condition evaluation failed: {} - {}", condition, conditionException.getMessage());
            logCollector.warning("Condition evaluation failed: {}", conditionException.getMessage());

            String next = getFirstOrNull(nextFalse);
            logCollector.info("Falling back to false branch due to evaluation error: {}", next);

            return ExecutionResult.nextNode(next);
        }
    }

    private String getFirstOrNull(List<String> list) {
        return (list != null && !list.isEmpty()) ? list.getFirst() : null;
    }
}