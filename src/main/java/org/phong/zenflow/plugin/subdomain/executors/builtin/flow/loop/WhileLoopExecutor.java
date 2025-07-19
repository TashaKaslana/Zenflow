package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.loop;

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
@AllArgsConstructor
@Slf4j
public class WhileLoopExecutor implements PluginNodeExecutor {
    @Override
    public String key() {
        return "core:flow.loop.while";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config) {
        Map<String, Object> input = ObjectConversion.convertObjectToMap(config.get("input"));
        String condition = (String) input.get("condition");
        List<String> next = ObjectConversion.safeConvert(input.get("next"), new TypeReference<>() {});
        List<String> loopEnd = ObjectConversion.safeConvert(input.get("loopEnd"), new TypeReference<>() {});

        if (condition == null || condition.isBlank()) {
            throw new IllegalArgumentException("WhileLoop condition is null or blank.");
        }

        Object result = AviatorEvaluator.execute(condition);
        if (!(result instanceof Boolean)) {
            throw new IllegalStateException("WhileLoop condition must evaluate to boolean, but got: " + result);
        }

        boolean isContinue = (Boolean) result;
        log.debug("WhileLoop evaluated condition [{}] to [{}]", condition, isContinue);

        if (!input.containsKey("next") || !input.containsKey("loopEnd")) {
            throw new IllegalStateException("WhileLoop node missing next or loopEnd target.");
        }

        return isContinue
                ? ExecutionResult.nextNode(next.getFirst())
                : ExecutionResult.nextNode(loopEnd.getFirst());
    }
}