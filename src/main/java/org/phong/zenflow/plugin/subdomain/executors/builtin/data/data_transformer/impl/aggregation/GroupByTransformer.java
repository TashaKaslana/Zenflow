package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation.AggregationUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GroupByTransformer implements DataTransformer {

    @Override
    public String getName() {
        return "group_by";
    }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        if (!(data instanceof List<?> list)) {
            throw new DataTransformerExecutorException("Input must be a List for group_by transformer.");
        }

        if (params == null || !params.containsKey("groupBy")) {
            throw new DataTransformerExecutorException("groupBy parameter is required for group_by transformer.");
        }

        List<String> groupByFields = extractGroupByFields(params);
        List<Map<String, Object>> aggregations = extractAggregations(params);

        // Group the data
        Map<String, List<Map<String, Object>>> groups = groupData(list, groupByFields);

        // Apply aggregations
        return groups.entrySet().stream()
                .map(entry -> createAggregatedResult(entry.getKey(), entry.getValue(), groupByFields, aggregations))
                .collect(Collectors.toList());
    }

    private List<String> extractGroupByFields(Map<String, Object> params) {
        Object groupByObj = params.get("groupBy");
        if (groupByObj instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toList());
        } else if (groupByObj instanceof String str) {
            return List.of(str);
        } else {
            throw new DataTransformerExecutorException("groupBy must be a string or list of strings.");
        }
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

    @SuppressWarnings("unchecked")
    private Map<String, List<Map<String, Object>>> groupData(List<?> list, List<String> groupByFields) {
        return list.stream()
                .map(item -> (Map<String, Object>) item)
                .collect(Collectors.groupingBy(item -> createGroupKey(item, groupByFields)));
    }

    private String createGroupKey(Map<String, Object> item, List<String> groupByFields) {
        return groupByFields.stream()
                .map(field -> String.valueOf(item.get(field)))
                .collect(Collectors.joining("||"));
    }

    private Map<String, Object> createAggregatedResult(String groupKey, List<Map<String, Object>> groupItems,
                                                      List<String> groupByFields, List<Map<String, Object>> aggregations) {
        Map<String, Object> result = new HashMap<>();

        // Add group by fields to result
        String[] keyParts = groupKey.split("\\|\\|");
        for (int i = 0; i < groupByFields.size() && i < keyParts.length; i++) {
            result.put(groupByFields.get(i), keyParts[i]);
        }

        // Apply aggregations
        for (Map<String, Object> aggregation : aggregations) {
            String field = (String) aggregation.get("field");
            String function = (String) aggregation.get("function");
            String alias = (String) aggregation.getOrDefault("alias", field + "_" + function);

            Object aggregatedValue = AggregationUtils.applyAggregation(groupItems, field, function);
            result.put(alias, aggregatedValue);
        }

        return result;
    }
}
