package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.for_loop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class ForLoopExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();

        // Read all parameters from context
        Object loopEndObj = context.read("loopEnd", Object.class);
        List<String> loopEnd = ObjectConversion.safeConvert(loopEndObj, new TypeReference<List<String>>() {});
        Object nextObj = context.read("next", Object.class);
        List<String> next = ObjectConversion.safeConvert(nextObj, new TypeReference<List<String>>() {});
        String breakCondition = context.read("breakCondition", String.class);
        String continueCondition = context.read("continueCondition", String.class);
        String endCondition = context.read("endCondition", String.class);
        Integer total = context.read("total", Integer.class);
        String updateExpression = context.read("updateExpression", String.class);
        Integer index = context.read("index", Integer.class);
        if (index == null) {
            index = 0;
        }

        // Create output that includes ALL necessary data for next iteration
        Map<String, Object> output = new HashMap<>();
        output.put("loopEnd", loopEnd);
        output.put("next", next);
        output.put("breakCondition", breakCondition);
        output.put("continueCondition", continueCondition);
        output.put("endCondition", endCondition);
        output.put("total", total);
        output.put("updateExpression", updateExpression);
        output.put("index", index);


        AviatorEvaluatorInstance evaluator = context.getEvaluator().cloneInstance();

        if (isLoopComplete(endCondition, total, output, logCollector, context, evaluator)) {
            logCollector.info("Loop finished. Proceeding to loopEnd.");
            if (loopEnd == null || loopEnd.isEmpty()) {
                logCollector.warning("loopEnd is empty, no next node to proceed to.");
                return ExecutionResult.loopEnd(null, output);
            }
            return ExecutionResult.loopEnd(loopEnd.getFirst(), output);
        }

        if (evalCondition(breakCondition, output, context, logCollector, evaluator)) {
            logCollector.info("Loop exited due to break condition at index {}", output.get("index"));
            if (loopEnd == null || loopEnd.isEmpty()) {
                logCollector.warning("loopEnd is empty, no next node to proceed to after break condition.");
                return ExecutionResult.loopBreak(null, output);
            }
            return ExecutionResult.loopBreak(loopEnd.getFirst(), output);
        }

        if (evalCondition(continueCondition, output, context, logCollector, evaluator)) {
            int newIndex = getNewIndex(updateExpression, output, context, logCollector, evaluator);
            output.put("index", newIndex);
            logCollector.info("Loop continued to next iteration due to continue condition.");
            return ExecutionResult.loopContinue(output);
        }

        int newIndex = getNewIndex(updateExpression, output, context, logCollector, evaluator);
        output.put("index", newIndex);

        logCollector.info("Proceeding to loop body for index {}. New index is {}", index, newIndex);
        if (next == null || next.isEmpty()) {
            logCollector.warning("next is empty, no next node to proceed to for loop body.");
            return ExecutionResult.loopNext(null, output);
        }
        return ExecutionResult.loopNext(next.getFirst(), output);
    }

    private boolean isLoopComplete(
        String endCondition, 
        Integer total, 
        Map<String, Object> context, 
        NodeLogPublisher logCollector, 
        ExecutionContext execCtx, 
        AviatorEvaluatorInstance evaluator
    ) {
        if (endCondition != null) {
            boolean end = evalCondition(endCondition, context, execCtx, logCollector, evaluator);
            if (end) {
                logCollector.info("Loop ended due to endCondition being met.");
            }
            return end;
        }

        if (total != null) {
            int index = ((Number) context.get("index")).intValue();
            boolean complete = index >= total;
            if (complete) {
                logCollector.info("Loop completed after {} iterations.", total);
            }
            return complete;
        }

        return false; // Should not be reached if validation passes
    }

    private int getNewIndex(String updateExpression, Map<String, Object> context, ExecutionContext execCtx, NodeLogPublisher logCollector, AviatorEvaluatorInstance evaluator) {
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
