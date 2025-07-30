package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.loop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ForEachLoopExecutor implements PluginNodeExecutor {

    @Override
    public String key() {
        return "core:flow.loop.foreach:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config) {
        LogCollector logCollector = new LogCollector();
        try {
            Map<String, Object> input = config.input();
            List<Object> items = ObjectConversion.safeConvert(input.get("items"), new TypeReference<>() {});
            int index = (int) input.getOrDefault("index", 0);

            if (index >= items.size()) {
                List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
                logCollector.info("Loop completed after {} iterations", items.size());
                return ExecutionResult.loopEnd(loopEnd.getFirst(), logCollector.getLogs());
            }

            Object currentItem = items.get(index);
            Map<String, Object> context = new HashMap<>();
            context.put("item", currentItem);
            context.put("index", index);

            if (evalCondition(input.get("breakCondition"), context, logCollector)) {
                List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
                logCollector.info("Break condition met at index {}, exiting loop", index);
                return ExecutionResult.loopBreak(loopEnd.getFirst(), context, logCollector.getLogs());
            }

            if (evalCondition(input.get("continueCondition"), context, logCollector)) {
                context.put("index", index + 1);
                logCollector.info("Continue condition met at index {}, skipping to next", index);
                return ExecutionResult.loopContinue(context, logCollector.getLogs());
            }

            List<String> next = ObjectConversion.safeConvert(input.get("next"), new TypeReference<>() {});
            context.put("index", index + 1); // Prepare for next iteration

            logCollector.info("Processing item {} of {}: {}", index + 1, items.size(), currentItem);
            return ExecutionResult.loopNext(next.getFirst(), context, logCollector.getLogs());

        } catch (Exception e) {
            log.error("Execution failed in ForEachLoop", e);
            logCollector.error("Execution failed: " + e.getMessage());
            return ExecutionResult.error("Execution failed: " + e.getMessage(), logCollector.getLogs());
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

