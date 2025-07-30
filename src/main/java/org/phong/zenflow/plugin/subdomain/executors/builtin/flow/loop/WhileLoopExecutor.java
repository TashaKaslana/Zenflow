package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.loop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class WhileLoopExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core:flow.loop.while:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config) {
        LogCollector logCollector = new LogCollector();
        try {
            Map<String, Object> input = config.input();

            boolean shouldContinue = evalCondition(input.get("condition"), input, logCollector);
            logCollector.info("While loop condition evaluated to [{}]", shouldContinue);

            if (!shouldContinue) {
                List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
                logCollector.info("While loop completed.");
                return ExecutionResult.loopEnd(loopEnd.getFirst(), input, logCollector.getLogs());
            }

            if (evalCondition(input.get("breakCondition"), input, logCollector)) {
                List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
                logCollector.info("Break condition met, exiting while loop.");
                return ExecutionResult.loopBreak(loopEnd.getFirst(), input, logCollector.getLogs());
            }

            if (evalCondition(input.get("continueCondition"), input, logCollector)) {
                logCollector.info("Continue condition met, skipping to next iteration.");
                return ExecutionResult.loopContinue(input, logCollector.getLogs());
            }

            List<String> next = ObjectConversion.safeConvert(input.get("next"), new TypeReference<>() {});
            logCollector.info("Proceeding to while loop body.");
            return ExecutionResult.loopNext(next.getFirst(), input, logCollector.getLogs());

        } catch (Exception e) {
            log.error("Failed to process while-loop", e);
            logCollector.error("Failed to process while-loop: " + e.getMessage());
            return ExecutionResult.error("Failed to process while-loop: " + e.getMessage(), logCollector.getLogs());
        }
    }

    private boolean evalCondition(Object rawExpr, Map<String, Object> context, LogCollector logCollector) {
        if (rawExpr instanceof String expr && !expr.isBlank()) {
            try {
                Object result = AviatorEvaluator.execute(expr, context);
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
