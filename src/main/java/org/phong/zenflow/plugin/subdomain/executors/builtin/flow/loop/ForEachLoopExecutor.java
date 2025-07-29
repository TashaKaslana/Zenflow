package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.loop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
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
            
            // Validate required fields
            if (!input.containsKey("iterator")) {
                String errorMsg = "ForEachLoop requires 'iterator' in config.";
                logCollector.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            if (!input.containsKey("next") || !input.containsKey("loopEnd")) {
                String errorMsg = "ForEachLoop requires both 'next' and 'loopEnd' arrays in config.";
                logCollector.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // Get system state injected by mediator
            Map<String, Object> loopState = ObjectConversion.convertObjectToMap(input.get("__system_state__"));
            
            Result result = getResult(input, loopState);
            List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
            List<String> next = ObjectConversion.safeConvert(input.get("next"), new TypeReference<>() {});

            // Check if loop is complete
            if (result.index() >= result.total()) {
                logCollector.info("ForEach loop completed after processing {} items", result.total());
                return ExecutionResult.nextNode(loopEnd.getFirst(), logCollector.getLogs());
            }

            // Prepare current iteration context
            Map<String, Object> output = new HashMap<>();
            List<?> items = (List<?>) result.loopState().get("items");
            Object currentItem = items.get(result.index());
            
            String itemVar = (String) input.getOrDefault("itemVar", "item");
            String indexVar = (String) input.getOrDefault("indexVar", "index");
            String hasNextVar = (String) input.getOrDefault("hasNextVar", "hasNext");
            String isFirstVar = (String) input.getOrDefault("isFirstVar", "isFirst");
            String isLastVar = (String) input.getOrDefault("isLastVar", "isLast");
            
            output.put(itemVar, currentItem);
            output.put(indexVar, result.index());
            output.put(hasNextVar, result.index() < result.total() - 1);
            output.put(isFirstVar, result.index() == 0);
            output.put(isLastVar, result.index() == result.total() - 1);

            logCollector.info("ForEach loop processing item {} of {}: {}", 
                            result.index() + 1, result.total(), currentItem);

            // Evaluate break condition
            if (evalCondition(input.get("breakCondition"), output)) {
                logCollector.info("ForEach loop exited due to break condition at item {}", result.index() + 1);
                return ExecutionResult.nextNode(loopEnd.getFirst(), output, logCollector.getLogs());
            }

            // Evaluate continue condition (skip current item)
            if (evalCondition(input.get("continueCondition"), output)) {
                result.loopState().put("index", result.index() + 1);
                output.put("__system_state__", result.loopState());
                logCollector.info("ForEach loop skipped item {} due to continue condition", result.index() + 1);
                return ExecutionResult.nextNode("__SELF__", output, logCollector.getLogs());
            }

            // Process current item - update state for next iteration
            result.loopState().put("index", result.index() + 1);
            output.put("__system_state__", result.loopState());
            
            logCollector.info("Proceeding to forEach loop body for item {}", result.index() + 1);
            return ExecutionResult.nextNode(next.getFirst(), output, logCollector.getLogs());

        } catch (Exception e) {
            log.error("Failed to process forEach-loop", e);
            logCollector.error("Failed to process forEach-loop: " + e.getMessage());
            return ExecutionResult.error("Failed to process forEach-loop: " + e.getMessage(), logCollector.getLogs());
        }
    }

    private static Result getResult(Map<String, Object> input, Map<String, Object> loopState) {
        if (loopState == null) {
            loopState = new HashMap<>();
            
            String iterator = (String) input.get("iterator");
            Object iterableRaw = input.get(iterator);
            
            if (!(iterableRaw instanceof List<?> items)) {
                throw new IllegalArgumentException("Iterator '" + iterator + "' must be a list, got: " + 
                    (iterableRaw != null ? iterableRaw.getClass().getSimpleName() : "null"));
            }

            if (items.isEmpty()) {
                throw new IllegalArgumentException("Iterator '" + iterator + "' cannot be empty for forEach loop");
            }

            loopState.put("items", items);
            loopState.put("index", 0);
            loopState.put("total", items.size());
        }

        int index = (int) loopState.get("index");
        int total = (int) loopState.get("total");
        return new Result(loopState, index, total);
    }

    private record Result(Map<String, Object> loopState, int index, int total) {}

    private boolean evalCondition(Object rawExpr, Map<String, Object> context) {
        if (rawExpr instanceof String expr && !expr.isBlank()) {
            try {
                String interpolated = TemplateEngine.resolveTemplate(expr, context).toString();
                Object result = AviatorEvaluator.execute(interpolated);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.warn("Failed to evaluate condition '{}': {}", rawExpr, e.getMessage());
                return false;
            }
        }
        return false;
    }
}
