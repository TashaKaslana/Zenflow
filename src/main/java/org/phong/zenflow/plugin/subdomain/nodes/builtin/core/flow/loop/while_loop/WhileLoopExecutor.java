package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.while_loop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@PluginNode(
        key = "core:flow.loop.while",
        name = "While Loop",
        version = "1.0.0",
        description = "Executes a loop while a specified condition is true. Supports break and continue conditions.",
        icon = "loop",
        type = "flow.loop",
        tags = {"flow", "loop", "while", "conditional"}
)
@AllArgsConstructor
@Slf4j
public class WhileLoopExecutor implements PluginNodeExecutor {
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();

            AviatorEvaluatorInstance evaluator = context.getEvaluator().clone();

            boolean shouldContinue = evalCondition(input.get("condition"), input, context, logCollector, evaluator);
            logCollector.info("While loop condition evaluated to [{}]", shouldContinue);

            if (!shouldContinue) {
                List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
                logCollector.info("While loop completed.");
                if (loopEnd.isEmpty()) {
                    logCollector.warning("loopEnd is empty, no next node to proceed to after completion.");
                    return ExecutionResult.loopEnd(null, input);
                }
                return ExecutionResult.loopEnd(loopEnd.getFirst(), input);
            }

            if (evalCondition(input.get("breakCondition"), input, context, logCollector, evaluator)) {
                List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
                logCollector.info("Break condition met, exiting while loop.");
                if (loopEnd.isEmpty()) {
                    logCollector.warning("loopEnd is empty, no next node to proceed to after break condition.");
                    return ExecutionResult.loopBreak(null, input);
                }
                return ExecutionResult.loopBreak(loopEnd.getFirst(), input);
            }

            if (evalCondition(input.get("continueCondition"), input, context, logCollector, evaluator)) {
                logCollector.info("Continue condition met, skipping to next iteration.");
                return ExecutionResult.loopContinue(input);
            }

            List<String> next = ObjectConversion.safeConvert(input.get("next"), new TypeReference<>() {});
            logCollector.info("Proceeding to while loop body.");
            if (next.isEmpty()) {
                logCollector.warning("next is empty, no next node to proceed to for loop body.");
                return ExecutionResult.loopNext(null, input);
            }
            return ExecutionResult.loopNext(next.getFirst(), input);

        } catch (Exception e) {
            logCollector.withException(e).error("Failed to process while-loop: {}", e.getMessage());
            return ExecutionResult.error("Failed to process while-loop: " + e.getMessage());
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
