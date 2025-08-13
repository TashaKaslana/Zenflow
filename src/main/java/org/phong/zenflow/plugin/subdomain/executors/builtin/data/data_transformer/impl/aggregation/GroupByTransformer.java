package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation;

import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
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

            Object aggregatedValue = applyAggregation(groupItems, field, function);
            result.put(alias, aggregatedValue);
        }

        return result;
    }

    private Object applyAggregation(List<Map<String, Object>> items, String field, String function) {
        return switch (function.toLowerCase()) {
            case "count" -> items.size();
            case "sum" -> calculateSum(items, field);
            case "avg", "average" -> calculateAverage(items, field);
            case "min" -> calculateMin(items, field);
            case "max" -> calculateMax(items, field);
            case "first" -> getFirst(items, field);
            case "last" -> getLast(items, field);
            case "concat" -> concatenateValues(items, field);
            case "distinct_count" -> countDistinct(items, field);
            case "std_dev" -> calculateStandardDeviation(items, field);
            default -> throw new DataTransformerExecutorException("Unsupported aggregation function: " + function);
        };
    }

    private double calculateSum(List<Map<String, Object>> items, String field) {
        return items.stream()
                .mapToDouble(item -> convertToDouble(item.get(field)))
                .sum();
    }

    private double calculateAverage(List<Map<String, Object>> items, String field) {
        return items.stream()
                .mapToDouble(item -> convertToDouble(item.get(field)))
                .average()
                .orElse(0.0);
    }

    private Object calculateMin(List<Map<String, Object>> items, String field) {
        return items.stream()
                .map(item -> item.get(field))
                .filter(Objects::nonNull)
                .min(this::compareValues)
                .orElse(null);
    }

    private Object calculateMax(List<Map<String, Object>> items, String field) {
        return items.stream()
                .map(item -> item.get(field))
                .filter(Objects::nonNull)
                .max(this::compareValues)
                .orElse(null);
    }

    private Object getFirst(List<Map<String, Object>> items, String field) {
        return items.isEmpty() ? null : items.getFirst().get(field);
    }

    private Object getLast(List<Map<String, Object>> items, String field) {
        return items.isEmpty() ? null : items.getLast().get(field);
    }

    private String concatenateValues(List<Map<String, Object>> items, String field) {
        return items.stream()
                .map(item -> String.valueOf(item.get(field)))
                .filter(value -> !"null".equals(value))
                .collect(Collectors.joining(", "));
    }

    private long countDistinct(List<Map<String, Object>> items, String field) {
        return items.stream()
                .map(item -> item.get(field))
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private double calculateStandardDeviation(List<Map<String, Object>> items, String field) {
        double mean = calculateAverage(items, field);
        double variance = items.stream()
                .mapToDouble(item -> {
                    double value = convertToDouble(item.get(field));
                    return Math.pow(value - mean, 2);
                })
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @SuppressWarnings("unchecked")
    private int compareValues(Object a, Object b) {
        int comparison = a.toString().compareTo(b.toString());
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                return ((Comparable<Object>) a).compareTo(b);
            } catch (ClassCastException e) {
                // Fallback to string comparison
                return comparison;
            }
        }
        return comparison;
    }
}
