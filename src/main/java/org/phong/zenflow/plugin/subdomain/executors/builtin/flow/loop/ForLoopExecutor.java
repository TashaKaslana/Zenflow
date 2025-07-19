package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.loop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ForLoopExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core:flow.loop.for";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config) {
        Map<String, Object> input = ObjectConversion.convertObjectToMap(config.get("input"));
        String stateKey = "__loop_state__:" + input.get("key");
        Map<String, Object> loopState = ObjectConversion.convertObjectToMap(config.get(stateKey));

        Result result = getResult(input, loopState);
        List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});

        Map<String, Object> output = new HashMap<>();

        if (result.index() >= result.total()) {
            return ExecutionResult.nextNode(loopEnd.getFirst());
        }

        // Set loop vars for the current iteration
        String indexVar = (String) input.getOrDefault("indexVar", "index");
        output.put(indexVar, result.index());

        if (result.loopState().containsKey("items")) {
            List<?> items = (List<?>) result.loopState().get("items");
            output.put((String) input.get("itemVar"), items.get(result.index()));
        }

        // Evaluate breakCondition
        if (evalCondition(input.get("breakCondition"), output)) {
            return ExecutionResult.nextNode(loopEnd.getFirst());
        }

        // Evaluate continueCondition
        if (evalCondition(input.get("continueCondition"), output)) {
            result.loopState().put("index", result.index() + 1);
            output.put(stateKey, result.loopState());
            return ExecutionResult.nextNode((String) input.get("key")); // re-enter same node (next iteration)
        }

        // Continue loop: update state and set next node
        result.loopState().put("index", result.index() + 1);
        output.put(stateKey, result.loopState());

        return ExecutionResult.nextNode(loopEnd.getFirst(), output);
    }

    private static Result getResult(Map<String, Object> config, Map<String, Object> loopState) {
        if (loopState == null) {
            loopState = new HashMap<>();
            int total;

            String iterator = (String) config.get("iterator");
            if (iterator != null) {
                Object iterableRaw = config.get(iterator);
                if (!(iterableRaw instanceof List<?> items)) {
                    throw new IllegalArgumentException("Iterator '" + iterator + "' is not a list.");
                }
                loopState.put("items", items);
                total = items.size();
            } else if (config.get("times") instanceof Number timesRaw) {
                total = timesRaw.intValue();
            } else {
                throw new IllegalArgumentException("ForLoop requires either 'iterator' or 'times' in config.");
            }

            loopState.put("index", 0);
            loopState.put("total", total);
        }

        int index = (int) loopState.get("index");
        int total = (int) loopState.get("total");
        return new Result(loopState, index, total);
    }

    private record Result(Map<String, Object> loopState, int index, int total) {
    }

    private boolean evalCondition(Object rawExpr, Map<String, Object> context) {
        if (rawExpr instanceof String expr && !expr.isBlank()) {
            String interpolated = TemplateEngine.resolveTemplate(expr, context).toString();
            Object result = AviatorEvaluator.execute(interpolated);
            return Boolean.TRUE.equals(result);
        }
        return false;
    }
}