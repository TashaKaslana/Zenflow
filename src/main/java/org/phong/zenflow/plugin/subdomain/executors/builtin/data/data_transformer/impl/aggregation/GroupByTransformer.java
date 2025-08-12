package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Groups a list of map objects by the provided fields. This transformer no longer performs
 * any aggregation logic. Instead, it returns a collection of groups where each group contains
 * the grouped field values along with the list of items that belong to that group. Any
 * aggregations should be handled by {@link AggregateTransformer}.
 */
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

        Map<String, List<Map<String, Object>>> groups = groupData(list, groupByFields);

        return groups.entrySet().stream()
                .map(entry -> createGroupResult(entry.getKey(), entry.getValue(), groupByFields))
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

    private Map<String, Object> createGroupResult(String groupKey, List<Map<String, Object>> groupItems,
                                                 List<String> groupByFields) {
        Map<String, Object> result = new HashMap<>();

        String[] keyParts = groupKey.split("\\|\\|");
        for (int i = 0; i < groupByFields.size() && i < keyParts.length; i++) {
            result.put(groupByFields.get(i), keyParts[i]);
        }

        result.put("items", groupItems);
        return result;
    }
}

