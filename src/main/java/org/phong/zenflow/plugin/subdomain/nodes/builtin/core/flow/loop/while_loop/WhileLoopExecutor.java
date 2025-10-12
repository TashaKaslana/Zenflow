package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.while_loop;

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
@AllArgsConstructor
@Slf4j
public class WhileLoopExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();

        String condition = context.read("condition", String.class);
        Object loopEndObj = context.read("loopEnd", Object.class);
        List<String> loopEnd = ObjectConversion.safeConvert(loopEndObj, new TypeReference<List<String>>() {});
        Object nextObj = context.read("next", Object.class);
        List<String> next = ObjectConversion.safeConvert(nextObj, new TypeReference<List<String>>() {});
        String breakCondition = context.read("breakCondition", String.class);
        String continueCondition = context.read("continueCondition", String.class);

        Map<String, Object> state = context.getCurrentNodeEntrypoint();

        AviatorEvaluatorInstance evaluator = context.getEvaluator().cloneInstance();

        boolean shouldContinue = evalCondition(condition, state, context, logCollector, evaluator);
        logCollector.info("While loop condition evaluated to [{}]", shouldContinue);

        if (!shouldContinue) {
            logCollector.info("While loop completed.");
            if (loopEnd == null || loopEnd.isEmpty()) {
                logCollector.warning("loopEnd is empty, no next node to proceed to after completion.");
                return ExecutionResult.loopEnd(null, state);
            }
            return ExecutionResult.loopEnd(loopEnd.getFirst(), state);
        }

        if (evalCondition(breakCondition, state, context, logCollector, evaluator)) {
            logCollector.info("Break condition met, exiting while loop.");
            if (loopEnd == null || loopEnd.isEmpty()) {
                logCollector.warning("loopEnd is empty, no next node to proceed to after break condition.");
                return ExecutionResult.loopBreak(null, state);
            }
            return ExecutionResult.loopBreak(loopEnd.getFirst(), state);
        }

        if (evalCondition(continueCondition, state, context, logCollector, evaluator)) {
            logCollector.info("Continue condition met, skipping to next iteration.");
            return ExecutionResult.loopContinue(state);
        }

        logCollector.info("Proceeding to while loop body.");
        if (next == null || next.isEmpty()) {
            logCollector.warning("next is empty, no next node to proceed to for loop body.");
            return ExecutionResult.loopNext(null, state);
        }
        return ExecutionResult.loopNext(next.getFirst(), state);
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
