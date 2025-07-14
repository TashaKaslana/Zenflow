package org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.loop.executor;

import com.googlecode.aviator.AviatorEvaluator;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator.loop.ForLoopDefinition;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ForLoopExecutor implements NodeExecutor<ForLoopDefinition> {

    @Override
    public String getNodeType() {
        return "for_loop";
    }

    @Override
    public ExecutionResult execute(ForLoopDefinition node, Map<String, Object> context) {
        String stateKey = "__loop_state__:" + node.getKey();
        Map<String, Object> loopState = ObjectConversion.convertObjectToMap(context.get(stateKey));

        Result result = getResult(node, context, loopState, stateKey);

        if (result.index() >= result.total()) {
            context.remove(stateKey);
            return ExecutionResult.nextNode(node.getLoopEnd().getFirst());
        }

        // Set loop vars
        String indexVar = (String) node.getConfig().getOrDefault("indexVar", "index");
        context.put(indexVar, result.index());

        if (result.loopState().containsKey("items")) {
            List<?> items = (List<?>) result.loopState().get("items");
            context.put(node.getItemVar(), items.get(result.index()));
        }

        // Evaluate breakCondition
        if (evalCondition(node.getConfig().get("breakCondition"), context)) {
            context.remove(stateKey);
            return ExecutionResult.nextNode(node.getLoopEnd().getFirst());
        }

        // Evaluate continueCondition
        if (evalCondition(node.getConfig().get("continueCondition"), context)) {
            result.loopState().put("index", result.index() + 1);
            return ExecutionResult.nextNode(node.getKey()); // re-enter same node (next iteration)
        }

        // Continue loop
        result.loopState().put("index", result.index() + 1);
        return ExecutionResult.nextNode(node.getNext().getFirst());
    }

    private static Result getResult(ForLoopDefinition node, Map<String, Object> context, Map<String, Object> loopState, String stateKey) {
        if (loopState == null) {
            loopState = new HashMap<>();
            int total;

            if (node.getIterator() != null) {
                Object iterableRaw = context.get(node.getIterator());
                if (!(iterableRaw instanceof List<?> items)) {
                    throw new IllegalArgumentException("Iterator '" + node.getIterator() + "' is not a list.");
                }
                loopState.put("items", items);
                total = items.size();
            } else if (node.getConfig().get("times") instanceof Number timesRaw) {
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


