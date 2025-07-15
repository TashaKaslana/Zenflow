package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.branch.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class IfNodeExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core.if";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config, Map<String, Object> context) {
        String rawCondition = (String) config.get("condition"); // e.g. "{{user.age}} > 18"
        List<String> nextTrue = ObjectConversion.safeConvert(config.get("nextTrue").toString(), new TypeReference<>() {
        });
        List<String> nextFalse = ObjectConversion.safeConvert(config.get("nextFalse").toString(), new TypeReference<>() {
        });

        // 1. Interpolate all templates first
        String interpolated = TemplateEngine.resolveTemplate(rawCondition, context).toString();
        if (interpolated == null || interpolated.isBlank()) {
            throw new IllegalArgumentException("If condition is null or blank after interpolation.");
        }
        log.debug("Resolved IF condition: {}", interpolated);

        // 2. Evaluate expression using Aviator
        boolean result;
        try {
            result = (Boolean) AviatorEvaluator.execute(interpolated);
        } catch (Exception e) {
            log.error("Failed to evaluate condition: {}", interpolated, e);
            throw new RuntimeException("Invalid condition expression: " + interpolated, e);
        }

        // 3. Branch
        String next = result
                ? getFirstOrNull(nextTrue)
                : getFirstOrNull(nextFalse);

        return ExecutionResult.nextNode(next);
    }

    private String getFirstOrNull(List<String> list) {
        return (list != null && !list.isEmpty()) ? list.getFirst() : null;
    }
}
