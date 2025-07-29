package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.loop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.AllArgsConstructor;
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

            // Validate required fields
            if (!input.containsKey("condition")) {
                String errorMsg = "WhileLoop requires 'condition' in config.";
                logCollector.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            if (!input.containsKey("next") || !input.containsKey("loopEnd")) {
                String errorMsg = "WhileLoop requires both 'next' and 'loopEnd' arrays in config.";
                logCollector.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // Get system state injected by mediator
            Map<String, Object> loopState = ObjectConversion.convertObjectToMap(input.get("__system_state__"));

            Result result = getResult(loopState);
            List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {
            });
            List<String> next = ObjectConversion.safeConvert(input.get("next"), new TypeReference<>() {
            });

            // Prepare output context for condition evaluation
            Map<String, Object> output = new HashMap<>();
            String iterationVar = (String) input.getOrDefault("iterationVar", "iteration");
            output.put(iterationVar, result.iteration());

            // Evaluate the while condition
            boolean shouldContinue = evalCondition(input.get("condition"), output);
            logCollector.info("While loop iteration {}: condition [{}] evaluated to [{}]",
                    result.iteration() + 1, input.get("condition"), shouldContinue);

            ExecutionResult specialCaseResult = hasSpecialCaseResult(shouldContinue, logCollector, result, loopEnd, output, input);
            if (specialCaseResult != null) {
                return specialCaseResult;
            }

            // Continue with loop body
            result.loopState().put("iteration", result.iteration() + 1);
            output.put("__system_state__", result.loopState());

            logCollector.info("Proceeding to while loop body for iteration {}", result.iteration());
            return ExecutionResult.nextNode(next.getFirst(), output, logCollector.getLogs());

        } catch (Exception e) {
            log.error("Failed to process while-loop", e);
            logCollector.error("Failed to process while-loop: " + e.getMessage());
            return ExecutionResult.error("Failed to process while-loop: " + e.getMessage(), logCollector.getLogs());
        }
    }

    private ExecutionResult hasSpecialCaseResult(boolean shouldContinue, LogCollector logCollector, Result result,
                                                 List<String> loopEnd, Map<String, Object> output, Map<String, Object> input) {
        if (!shouldContinue) {
            logCollector.info("While loop completed after {} iterations", result.iteration());
            return ExecutionResult.nextNode(loopEnd.getFirst(), output, logCollector.getLogs());
        }

        // Check for infinite loop protection
        int maxIterations = (int) input.getOrDefault("maxIterations", 1000);
        if (result.iteration() >= maxIterations) {
            String errorMsg = String.format("While loop exceeded maximum iterations (%d). Possible infinite loop.", maxIterations);
            logCollector.error(errorMsg);
            return ExecutionResult.error(errorMsg, logCollector.getLogs());
        }

        // Evaluate break condition (optional)
        if (evalCondition(input.get("breakCondition"), output)) {
            logCollector.info("While loop exited due to break condition at iteration {}", result.iteration() + 1);
            return ExecutionResult.nextNode(loopEnd.getFirst(), output, logCollector.getLogs());
        }

        // Evaluate continue condition (optional)
        if (evalCondition(input.get("continueCondition"), output)) {
            result.loopState().put("iteration", result.iteration() + 1);
            output.put("__system_state__", result.loopState());
            logCollector.info("While loop continued to next iteration due to continue condition");
            return ExecutionResult.nextNode("__SELF__", output, logCollector.getLogs());
        }
        return null;
    }

    private boolean evalCondition(Object rawExpr, Map<String, Object> context) {
        if (rawExpr instanceof String expr && !expr.isBlank()) {
            try {
                String interpolated = TemplateEngine.resolveTemplate(expr, context).toString();
                Object result = AviatorEvaluator.execute(interpolated);
                if (!(result instanceof Boolean)) {
                    log.warn("Condition '{}' evaluated to non-boolean value: {}", rawExpr, result);
                    return false;
                }
                return (Boolean) result;
            } catch (Exception e) {
                log.warn("Failed to evaluate condition '{}': {}", rawExpr, e.getMessage());
                return false;
            }
        }
        return false;
    }

    private record Result(Map<String, Object> loopState, int iteration) {
    }

    private static Result getResult(Map<String, Object> loopState) {
        if (loopState == null) {
            loopState = new HashMap<>();
            loopState.put("iteration", 0);
        }

        int iteration = (int) loopState.getOrDefault("iteration", 0);
        return new Result(loopState, iteration);
    }
}
