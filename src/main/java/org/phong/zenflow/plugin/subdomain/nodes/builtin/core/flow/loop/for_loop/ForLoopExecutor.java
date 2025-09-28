package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.for_loop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@PluginNode(
        key = "core:flow.loop.for",
        name = "For Loop",
        version = "1.0.0",
        description = "Executes a loop with a defined start, end, and increment. Supports break and continue conditions.",
        type = "flow.loop",
        tags = {"loop", "flow", "iteration"},
        icon = "ph:repeat"
)
@Slf4j
@AllArgsConstructor
public class ForLoopExecutor implements PluginNodeExecutor {
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();

            // Create output that includes ALL necessary data for next iteration
            Map<String, Object> output = new HashMap<>(input);

            AviatorEvaluatorInstance evaluator = context.getEvaluator().cloneInstance();

            if (isLoopComplete(input, output, logCollector, context, evaluator)) {
                List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
                logCollector.info("Loop finished. Proceeding to loopEnd.");
                if (loopEnd.isEmpty()) {
                    logCollector.warning("loopEnd is empty, no next node to proceed to.");
                    return ExecutionResult.loopEnd(null, output);
                }
                return ExecutionResult.loopEnd(loopEnd.getFirst(), output);
            }

            if (evalCondition(input.get("breakCondition"), output, context, logCollector, evaluator)) {
                List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
                logCollector.info("Loop exited due to break condition at index {}", output.get("index"));
                if (loopEnd.isEmpty()) {
                    logCollector.warning("loopEnd is empty, no next node to proceed to after break condition.");
                    return ExecutionResult.loopBreak(null, output);
                }
                return ExecutionResult.loopBreak(loopEnd.getFirst(), output);
            }

            if (evalCondition(input.get("continueCondition"), output, context, logCollector, evaluator)) {
                int newIndex = getNewIndex(input, output, context, logCollector, evaluator);
                output.put("index", newIndex);
                logCollector.info("Loop continued to next iteration due to continue condition.");
                return ExecutionResult.loopContinue(output);
            }

            List<String> next = ObjectConversion.safeConvert(input.get("next"), new TypeReference<>() {});
            int newIndex = getNewIndex(input, output, context, logCollector, evaluator);
            output.put("index", newIndex);

            logCollector.info("Proceeding to loop body for index {}. New index is {}", input.get("index"), newIndex);
            if (next.isEmpty()) {
                logCollector.warning("next is empty, no next node to proceed to for loop body.");
                return ExecutionResult.loopNext(null, output);
            }
            return ExecutionResult.loopNext(next.getFirst(), output);

        } catch (Exception e) {
            logCollector.withException(e).error("Failed to process for-loop: {}", e.getMessage());
            return ExecutionResult.error("Failed to process for-loop: " + e.getMessage());
        }
    }

    private boolean isLoopComplete(Map<String, Object> input, Map<String, Object> context, NodeLogPublisher logCollector, ExecutionContext execCtx, AviatorEvaluatorInstance evaluator) {
        if (input.containsKey("endCondition")) {
            boolean end = evalCondition(input.get("endCondition"), context, execCtx, logCollector, evaluator);
            if (end) {
                logCollector.info("Loop ended due to endCondition being met.");
            }
            return end;
        }

        if (input.containsKey("total")) {
            int index = ((Number) context.get("index")).intValue();
            int total = ((Number) input.get("total")).intValue();
            boolean complete = index >= total;
            if (complete) {
                logCollector.info("Loop completed after {} iterations.", total);
            }
            return complete;
        }

        return false; // Should not be reached if validation passes
    }

    private int getNewIndex(Map<String, Object> input, Map<String, Object> context, ExecutionContext execCtx, NodeLogPublisher logCollector, AviatorEvaluatorInstance evaluator) {
        String updateExpression = (String) input.get("updateExpression");
        try {
            Map<String, Object> env = new HashMap<>(context);
            env.put("context", execCtx);
            Object newIndex = evaluator.execute(updateExpression, env);
            return ((Number) newIndex).intValue();
        } catch (Exception e) {
            log.error("Failed to evaluate updateExpression '{}': {}", updateExpression, e.getMessage());
            logCollector.error("Failed to evaluate updateExpression: {}", e.getMessage());
            throw new IllegalStateException("Failed to update loop index", e);
        }
    }

    private boolean evalCondition(Object rawExpr, Map<String, Object> context, ExecutionContext execCtx, NodeLogPublisher logCollector, AviatorEvaluatorInstance evaluator) {
        if (rawExpr instanceof String expr && !expr.isBlank()) {
            try {
                Map<String, Object> env = new HashMap<>(context);
                env.put("context", execCtx);
                Object result = evaluator.execute(expr, env);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.warn("Failed to evaluate condition '{}': {}", rawExpr, e.getMessage());
                logCollector.warning("Failed to evaluate condition '{}': {}", rawExpr, e.getMessage());
                return false;
            }
        }
        return false;
    }
}
