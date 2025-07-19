package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.branch.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class IfNodeExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core:flow.branch.if";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config) {
        Map<String, Object> input = ObjectConversion.convertObjectToMap(config.get("input"));
        String condition = (String) input.get("condition"); // e.g. "true", "1 > 0"
        List<String> nextTrue = ObjectConversion.safeConvert(input.get("nextTrue").toString(), new TypeReference<>() {
        });
        List<String> nextFalse = ObjectConversion.safeConvert(input.get("nextFalse").toString(), new TypeReference<>() {
        });

        if (condition == null || condition.isBlank()) {
            throw new IllegalArgumentException("If condition is null or blank.");
        }
        log.debug("Evaluating IF condition: {}", condition);

        // Evaluate expression using Aviator
        boolean result;
        try {
            result = (Boolean) AviatorEvaluator.execute(condition);
        } catch (Exception e) {
            log.error("Failed to evaluate condition: {}", condition, e);
            throw new RuntimeException("Invalid condition expression: " + condition, e);
        }

        // Branch
        String next = result
                ? getFirstOrNull(nextTrue)
                : getFirstOrNull(nextFalse);

        return ExecutionResult.nextNode(next);
    }

    private String getFirstOrNull(List<String> list) {
        return (list != null && !list.isEmpty()) ? list.getFirst() : null;
    }
}