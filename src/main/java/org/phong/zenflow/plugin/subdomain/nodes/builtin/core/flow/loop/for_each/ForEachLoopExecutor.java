package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.for_each;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@PluginNode(
        key = "core:flow.loop.foreach",
        name = "For Each Loop",
        version = "1.0.0",
        description = "Executes a loop for each item in a list, allowing for break and continue conditions.",
        type = "flow.loop",
        tags = {"core", "flow", "loop", "foreach"},
        icon = "ph:repeat"
)
@Slf4j
public class ForEachLoopExecutor implements PluginNodeExecutor {

    @Override
    public String key() {
        return "core:flow.loop.foreach:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        LogCollector logCollector = new LogCollector();
        try {
            Map<String, Object> input = config.input();
            List<Object> items = ObjectConversion.safeConvert(input.get("items"), new TypeReference<>() {});
            int index = (int) input.getOrDefault("index", 0);

            if (index >= items.size()) {
                List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
                logCollector.info("Loop completed after {} iterations", items.size());
                if (loopEnd.isEmpty()) {
                    logCollector.warning("loopEnd is empty, no next node to proceed to after completion.");
                    return ExecutionResult.loopEnd(null, logCollector.getLogs());
                }
                return ExecutionResult.loopEnd(loopEnd.getFirst(), logCollector.getLogs());
            }

            Object currentItem = items.get(index);

            // Create output that includes ALL necessary data for next iteration
            Map<String, Object> output = new HashMap<>();
            // Preserve original input data
            output.put("items", items);
            output.put("loopEnd", input.get("loopEnd"));
            output.put("next", input.get("next"));
            output.put("breakCondition", input.get("breakCondition"));
            output.put("continueCondition", input.get("continueCondition"));
            // Add current iteration data
            output.put("item", currentItem);
            output.put("index", index);

            if (evalCondition(input.get("breakCondition"), output, logCollector)) {
                List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});
                logCollector.info("Break condition met at index {}, exiting loop", index);
                if (loopEnd.isEmpty()) {
                    logCollector.warning("loopEnd is empty, no next node to proceed to after break condition.");
                    return ExecutionResult.loopBreak(null, output, logCollector.getLogs());
                }
                return ExecutionResult.loopBreak(loopEnd.getFirst(), output, logCollector.getLogs());
            }

            if (evalCondition(input.get("continueCondition"), output, logCollector)) {
                output.put("index", index + 1);
                logCollector.info("Continue condition met at index {}, skipping to next", index);
                return ExecutionResult.loopContinue(output, logCollector.getLogs());
            }

            List<String> next = ObjectConversion.safeConvert(input.get("next"), new TypeReference<>() {});
            output.put("index", index + 1); // Prepare for next iteration

            logCollector.info("Processing item {} of {}: {}", index + 1, items.size(), currentItem);
            if (next.isEmpty()) {
                logCollector.warning("next is empty, no next node to proceed to for loop body.");
                return ExecutionResult.loopNext(null, output, logCollector.getLogs());
            }
            return ExecutionResult.loopNext(next.getFirst(), output, logCollector.getLogs());

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
