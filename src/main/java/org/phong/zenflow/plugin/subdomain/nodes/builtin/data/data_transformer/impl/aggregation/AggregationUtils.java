package org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.aggregation;

import lombok.NoArgsConstructor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.exception.DataTransformerExecutorException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility methods shared across aggregation based transformers.
 */
@NoArgsConstructor
public final class AggregationUtils {

    public static Object applyAggregation(List<Map<String, Object>> items, String field, String function) {
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

    private static double calculateSum(List<Map<String, Object>> items, String field) {
        return items.stream()
                .mapToDouble(item -> convertToDouble(item.get(field)))
                .sum();
    }

    private static double calculateAverage(List<Map<String, Object>> items, String field) {
        return items.stream()
                .mapToDouble(item -> convertToDouble(item.get(field)))
                .average()
                .orElse(0.0);
    }

    private static Object calculateMin(List<Map<String, Object>> items, String field) {
        return items.stream()
                .map(item -> item.get(field))
                .filter(Objects::nonNull)
                .min(AggregationUtils::compareValues)
                .orElse(null);
    }

    private static Object calculateMax(List<Map<String, Object>> items, String field) {
        return items.stream()
                .map(item -> item.get(field))
                .filter(Objects::nonNull)
                .max(AggregationUtils::compareValues)
                .orElse(null);
    }

    private static Object getFirst(List<Map<String, Object>> items, String field) {
        return items.isEmpty() ? null : items.getFirst().get(field);
    }

    private static Object getLast(List<Map<String, Object>> items, String field) {
        return items.isEmpty() ? null : items.getLast().get(field);
    }

    private static String concatenateValues(List<Map<String, Object>> items, String field) {
        return items.stream()
                .map(item -> String.valueOf(item.get(field)))
                .filter(value -> !"null".equals(value))
                .collect(Collectors.joining(", "));
    }

    private static long countDistinct(List<Map<String, Object>> items, String field) {
        return items.stream()
                .map(item -> item.get(field))
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private static double calculateStandardDeviation(List<Map<String, Object>> items, String field) {
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

    private static double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @SuppressWarnings("unchecked")
    private static int compareValues(Object a, Object b) {
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
