package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.branch.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.plugin.subdomain.executors.builtin.flow.branch.dto.ConditionalCase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class ConditionNodeExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core.condition";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config, Map<String, Object> context) {
        try {
            List<ConditionalCase> cases = ObjectConversion.safeConvert(config.get("cases").toString(), new TypeReference<>() {
            });
            for (ConditionalCase caseDef : cases) {
                String rawCondition = caseDef.when();
                String interpolated = TemplateEngine.resolveTemplate(rawCondition, context).toString();
                if (interpolated == null || interpolated.isBlank()) {
                    throw new IllegalArgumentException("Node condition is null or blank after interpolation.");
                }
                log.debug("Resolved condition: {}", interpolated);

                Boolean isMatch = (Boolean) AviatorEvaluator.execute(interpolated);
                if (isMatch) {
                    return ExecutionResult.nextNode(caseDef.then());
                }
            }
            return ExecutionResult.nextNode(config.get("default_case").toString());
        } catch (Exception e) {
            log.error("Failed to parse cases from context", e);
            throw new RuntimeException("Invalid cases format in context", e);
        }
    }
}
