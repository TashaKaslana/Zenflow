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
    public ExecutionResult execute(Map<String, Object> config, Map<String, Object> context) {
        String stateKey = "__loop_state__:" + config.get("key");
        Map<String, Object> loopState = ObjectConversion.convertObjectToMap(context.get(stateKey));

        Result result = getResult(config, context, loopState, stateKey);
        List<String> loopEnd = ObjectConversion.safeConvert(config.get("loopEnd"), new TypeReference<>() {});

        if (result.index() >= result.total()) {
            context.remove(stateKey);
            return ExecutionResult.nextNode(loopEnd.getFirst());
        }

        // Set loop vars
        String indexVar = (String) config.getOrDefault("indexVar", "index");
        context.put(indexVar, result.index());

        if (result.loopState().containsKey("items")) {
            List<?> items = (List<?>) result.loopState().get("items");
            context.put((String) config.get("itemVar"), items.get(result.index()));
        }

        // Evaluate breakCondition
        if (evalCondition(config.get("breakCondition"), context)) {
            context.remove(stateKey);
            return ExecutionResult.nextNode(loopEnd.getFirst());
        }

        // Evaluate continueCondition
        if (evalCondition(config.get("continueCondition"), context)) {
            result.loopState().put("index", result.index() + 1);
            return ExecutionResult.nextNode((String) config.get("key")); // re-enter same node (next iteration)
        }

        // Continue loop
        result.loopState().put("index", result.index() + 1);
        return ExecutionResult.nextNode(loopEnd.getFirst());
    }

    private static Result getResult(Map<String, Object> config, Map<String, Object> context, Map<String, Object> loopState, String stateKey) {
        if (loopState == null) {
            loopState = new HashMap<>();
            int total;

            String iterator = (String) config.get("iterator");
            if (iterator != null) {
                Object iterableRaw = context.get(iterator);
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
            context.put(stateKey, loopState);
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
