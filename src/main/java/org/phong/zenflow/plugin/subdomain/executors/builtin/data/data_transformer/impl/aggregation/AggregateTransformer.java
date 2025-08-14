package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates a list of records without grouping.
 */
@Component
public class AggregateTransformer implements DataTransformer {
    @Override
    public String getName() {
        return "aggregate";
    }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        if (!(data instanceof List<?> list)) {
            throw new DataTransformerExecutorException("Input must be a List for aggregate transformer.");
        }
        if (params == null || !params.containsKey("aggregations")) {
            throw new DataTransformerExecutorException("aggregations parameter is required for aggregate transformer.");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) list;
        List<Map<String, Object>> aggregations = extractAggregations(params);
        Map<String, Object> result = new HashMap<>();

        for (Map<String, Object> aggregation : aggregations) {
            String field = (String) aggregation.get("field");
            String function = (String) aggregation.get("function");
            String alias = (String) aggregation.getOrDefault("alias", field + "_" + function);
            Object value = AggregationUtils.applyAggregation(items, field, function);
            result.put(alias, value);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractAggregations(Map<String, Object> params) {
        Object aggregationsObj = params.get("aggregations");
        if (aggregationsObj == null) {
            return Collections.emptyList();
        }

        if (aggregationsObj instanceof List<?> list) {
            return list.stream()
                    .map(item -> (Map<String, Object>) item)
                    .collect(Collectors.toList());
        }

        throw new DataTransformerExecutorException("aggregations must be a list of aggregation objects.");
    }
}
