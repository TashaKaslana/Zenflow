package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl.aggregation;

import com.googlecode.aviator.AviatorEvaluatorInstance;
import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.interfaces.DataTransformer;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class FilterTransformer implements DataTransformer {
    private final TemplateService templateService;

    @Override
    public String getName() {
        return "filter";
    }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        if (!(data instanceof List<?> list)) {
            throw new DataTransformerExecutorException("Input must be a List for filter transformer.");
        }

        if (params == null || !params.containsKey("expression")) {
            throw new DataTransformerExecutorException("Expression parameter is required for filter transformer.");
        }

        String expression = (String) params.get("expression");
        String mode = (String) params.getOrDefault("mode", "include");

        AviatorEvaluatorInstance evaluator = templateService.newChildEvaluator();

        return list.stream()
                .filter(item -> evaluateFilter(item, expression, mode, evaluator))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateFilter(Object item, String expression, String mode, AviatorEvaluatorInstance evaluator) {
        try {
            Map<String, Object> context = new HashMap<>();

            if (item instanceof Map<?, ?> map) {
                // For maps, add all fields to context
                context.putAll((Map<String, Object>) map);
            } else {
                // For primitive values, add as 'value' variable
                context.put("value", item);
            }

            // Add common utility variables
            context.put("item", item);

            Object result = evaluator.execute(expression, context);
            boolean matches = convertToBoolean(result);

            // Include mode: return items that match
            // Exclude mode: return items that don't match
            return "exclude".equalsIgnoreCase(mode) != matches;

        } catch (Exception e) {
            throw new DataTransformerExecutorException("Error evaluating filter expression: " + e.getMessage());
        }
    }

    private boolean convertToBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number num) {
            return num.doubleValue() != 0.0;
        }
        if (value instanceof String str) {
            return !str.isEmpty() && !"false".equalsIgnoreCase(str) && !"0".equals(str);
        }
        return value != null;
    }
}
