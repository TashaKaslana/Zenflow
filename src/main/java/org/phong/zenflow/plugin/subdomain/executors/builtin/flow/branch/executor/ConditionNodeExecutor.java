package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.branch.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
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
        return "core:flow.branch.condition";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config) {
        try {
            Map<String, Object> input = ObjectConversion.convertObjectToMap(config.get("input"));
            List<ConditionalCase> cases = ObjectConversion.safeConvert(input.get("cases").toString(), new TypeReference<>() {
            });
            for (ConditionalCase caseDef : cases) {
                String condition = caseDef.when();
                if (condition == null || condition.isBlank()) {
                    throw new IllegalArgumentException("Node condition is null or blank.");
                }
                log.debug("Evaluating condition: {}", condition);

                Boolean isMatch = (Boolean) AviatorEvaluator.execute(condition);
                if (isMatch) {
                    return ExecutionResult.nextNode(caseDef.then());
                }
            }
            return ExecutionResult.nextNode(input.get("default_case").toString());
        } catch (Exception e) {
            log.error("Failed to parse or evaluate cases", e);
            throw new RuntimeException("Invalid cases format or condition expression", e);
        }
    }
}