package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.for_each;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.ReadOptions;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class ForEachLoopExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();

        Object itemsObj = context.read("items", Object.class);
        List<Object> items = ObjectConversion.safeConvert(itemsObj, new TypeReference<List<Object>>() {});
        if (items == null) {
            items = List.of();
        }
        
        Integer index = context.readOrDefault(
            "index", 
            Integer.class, 
            0,
            ReadOptions.PREFER_CONTEXT
        );

        Object loopEndObj = context.read("loopEnd", Object.class);
        List<String> loopEnd = ObjectConversion.safeConvert(loopEndObj, new TypeReference<>() {
        });
        Object nextObj = context.read("next", Object.class);
        List<String> next = ObjectConversion.safeConvert(nextObj, new TypeReference<>() {
        });
        String breakCondition = context.read("breakCondition", String.class);
        String continueCondition = context.read("continueCondition", String.class);

        AviatorEvaluatorInstance evaluator = context.getEvaluator().cloneInstance();

        if (index >= items.size()) {
            logCollector.info("Loop completed after {} iterations", items.size());
            if (loopEnd == null || loopEnd.isEmpty()) {
                logCollector.warning("loopEnd is empty, no next node to proceed to after completion.");
                return ExecutionResult.loopEnd(null);
            }
            return ExecutionResult.loopEnd(loopEnd.getFirst());
        }

        Object currentItem = items.get(index);

        // Write loop state to context for access by other nodes
        context.write("items", items);
        context.write("loopEnd", loopEnd);
        context.write("next", next);
        context.write("breakCondition", breakCondition);
        context.write("continueCondition", continueCondition);
        context.write("item", currentItem);
        context.write("index", index);

        // Create local map for condition evaluation
        Map<String, Object> loopState = new HashMap<>();
        loopState.put("items", items);
        loopState.put("loopEnd", loopEnd);
        loopState.put("next", next);
        loopState.put("breakCondition", breakCondition);
        loopState.put("continueCondition", continueCondition);
        loopState.put("item", currentItem);
        loopState.put("index", index);

        if (evalCondition(breakCondition, loopState, context, logCollector, evaluator)) {
            logCollector.info("Break condition met at index {}, exiting loop", index);
            if (loopEnd == null || loopEnd.isEmpty()) {
                logCollector.warning("loopEnd is empty, no next node to proceed to after break condition.");
                return ExecutionResult.loopBreak(null);
            }
            return ExecutionResult.loopBreak(loopEnd.getFirst());
        }

        if (evalCondition(continueCondition, loopState, context, logCollector, evaluator)) {
            context.write("index", index + 1);
            logCollector.info("Continue condition met at index {}, skipping to next", index);
            return ExecutionResult.loopContinue();
        }

        context.write("index", index + 1); // Prepare for next iteration

        logCollector.info("Processing item {} of {}: {}", index + 1, items.size(), currentItem);
        if (next == null || next.isEmpty()) {
            logCollector.warning("next is empty, no next node to proceed to for loop body.");
            return ExecutionResult.loopNext(null);
        }
        return ExecutionResult.loopNext(next.getFirst());
    }

    private boolean evalCondition(
        Object rawExpr, 
        Map<String, Object> context, 
        ExecutionContext execCtx, 
        NodeLogPublisher logCollector,
        AviatorEvaluatorInstance evaluator
    ) {
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
