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
    public ExecutionResult execute(ForLoopDefinition node, Map<String, Object> context) {
        String stateKey = "__loop_state__:" + node.getKey();
        Map<String, Object> loopState = ObjectConversion.convertObjectToMap(context.get(stateKey));

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

        if (index >= total) {
            context.remove(stateKey);
            return ExecutionResult.nextNode(node.getLoopEnd().getFirst());
        }

        // index variable
        String indexVar = (String) node.getConfig().getOrDefault("indexVar", "index");
        context.put(indexVar, index);

        if (loopState.containsKey("items")) {
            List<?> items = (List<?>) loopState.get("items");
            context.put(node.getItemVar(), items.get(index));
        }

        Object breakRaw = node.getConfig().get("breakCondition");
        if (breakRaw instanceof String rawCondition && !rawCondition.isBlank()) {
            String interpolated = TemplateEngine.resolveTemplate(rawCondition, context).toString();
            Object result = AviatorEvaluator.execute(interpolated);
            if (Boolean.TRUE.equals(result)) {
                context.remove(stateKey); // clear loop state
                return ExecutionResult.nextNode(node.getLoopEnd().getFirst());
            }
        }

        //continue to next iteration
        loopState.put("index", index + 1);

        return ExecutionResult.nextNode(node.getNext().getFirst());
    }
}


