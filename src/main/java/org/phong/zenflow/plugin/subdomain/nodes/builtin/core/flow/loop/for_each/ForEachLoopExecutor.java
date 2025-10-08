package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.for_each;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class ForEachLoopExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        Map<String, Object> input = config.input();
        List<Object> items = ObjectConversion.safeConvert(input.get("items"), new TypeReference<>() {});
        int index = (int) input.getOrDefault("index", 0);

        AviatorEvaluatorInstance evaluator = context.getEvaluator().cloneInstance();

        if (index >= items.size()) {
            List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
            logCollector.info("Loop completed after {} iterations", items.size());
            if (loopEnd.isEmpty()) {
                logCollector.warning("loopEnd is empty, no next node to proceed to after completion.");
                return ExecutionResult.loopEnd(null);
            }
            return ExecutionResult.loopEnd(loopEnd.getFirst());
        }

        Object currentItem = items.get(index);

        // Create output that includes ALL necessary data for next iteration
        Map<String, Object> output = new HashMap<>();
        // Preserve original input data
        output.put("items", items);
        output.put("loopEnd", input.get("loopEnd"));
        output.put("next", input.get("next"));
        output.put("breakCondition", input.get("breakCondition"));
        output.put("continueCondition", input.get("continueCondition"));
        // Add current iteration data
        output.put("item", currentItem);
        output.put("index", index);

        if (evalCondition(input.get("breakCondition"), output, context, logCollector, evaluator)) {
            List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
            logCollector.info("Break condition met at index {}, exiting loop", index);
            if (loopEnd.isEmpty()) {
                logCollector.warning("loopEnd is empty, no next node to proceed to after break condition.");
                return ExecutionResult.loopBreak(null, output);
            }
            return ExecutionResult.loopBreak(loopEnd.getFirst(), output);
        }

        if (evalCondition(input.get("continueCondition"), output, context, logCollector, evaluator)) {
            output.put("index", index + 1);
            logCollector.info("Continue condition met at index {}, skipping to next", index);
            return ExecutionResult.loopContinue(output);
        }

        List<String> next = ObjectConversion.safeConvert(input.get("next"), new TypeReference<>() {});
        output.put("index", index + 1); // Prepare for next iteration

        logCollector.info("Processing item {} of {}: {}", index + 1, items.size(), currentItem);
        if (next.isEmpty()) {
            logCollector.warning("next is empty, no next node to proceed to for loop body.");
            return ExecutionResult.loopNext(null, output);
        }
        return ExecutionResult.loopNext(next.getFirst(), output);
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
