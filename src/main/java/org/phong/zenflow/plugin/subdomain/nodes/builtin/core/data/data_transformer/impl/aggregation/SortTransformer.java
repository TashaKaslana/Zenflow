package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl.aggregation;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SortTransformer implements DataTransformer {

    @Override
    public String getName() {
        return "sort";
    }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        if (!(data instanceof List<?> list)) {
            throw new DataTransformerExecutorException("Input must be a List for sort transformer.");
        }

        if (params == null) {
            throw new DataTransformerExecutorException("Sort parameters are required.");
        }

        // Support both single field and multiple fields sorting
        List<Map<String, Object>> sortFields = extractSortFields(params);
        boolean nullsFirst = (Boolean) params.getOrDefault("nullsFirst", false);

        return list.stream()
                .map(ObjectConversion::convertObjectToMap)
                .sorted(createComparator(sortFields, nullsFirst))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractSortFields(Map<String, Object> params) {
        List<Map<String, Object>> sortFields = new ArrayList<>();

        if (params.containsKey("fields")) {
            Object fieldsObj = params.get("fields");
            if (fieldsObj instanceof List<?> fieldsList) {
                for (Object fieldObj : fieldsList) {
                    if (fieldObj instanceof Map<?, ?> fieldMap) {
                        sortFields.add((Map<String, Object>) fieldMap);
                    }
                }
            }
        } else if (params.containsKey("field")) {
            // Single field support for backward compatibility
            Map<String, Object> singleField = new HashMap<>();
            singleField.put("field", params.get("field"));
            singleField.put("order", params.getOrDefault("order", "asc"));
            singleField.put("type", params.getOrDefault("type", "string"));
            singleField.put("caseSensitive", params.getOrDefault("caseSensitive", true));
            sortFields.add(singleField);
        } else {
            throw new DataTransformerExecutorException("Either 'field' or 'fields' parameter is required for sort transformer.");
        }

        return sortFields;
    }

    private Comparator<Map<String, Object>> createComparator(List<Map<String, Object>> sortFields, boolean nullsFirst) {
        Comparator<Map<String, Object>> comparator = null;

        for (Map<String, Object> sortField : sortFields) {
            String field = (String) sortField.get("field");
            String order = (String) sortField.getOrDefault("order", "asc");
            String type = (String) sortField.getOrDefault("type", "string");
            boolean caseSensitive = (Boolean) sortField.getOrDefault("caseSensitive", true);

            Comparator<Map<String, Object>> fieldComparator = createFieldComparator(field, type, caseSensitive, nullsFirst);

            if ("desc".equalsIgnoreCase(order)) {
                fieldComparator = fieldComparator.reversed();
            }

            if (comparator == null) {
                comparator = fieldComparator;
            } else {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }

        return comparator != null ? comparator : (a, b) -> 0;
    }

    private Comparator<Map<String, Object>> createFieldComparator(String field, String type, boolean caseSensitive, boolean nullsFirst) {
        return (a, b) -> {
            Object valueA = a.get(field);
            Object valueB = b.get(field);

            if (valueA == null && valueB == null) return 0;
            if (valueA == null) return nullsFirst ? -1 : 1;
            if (valueB == null) return nullsFirst ? 1 : -1;

            try {
                return switch (type.toLowerCase()) {
                    case "number", "integer", "int" -> compareNumbers(valueA, valueB);
                    case "date", "datetime" -> compareDates(valueA, valueB);
                    case "boolean", "bool" -> compareBooleans(valueA, valueB);
                    default -> compareStrings(valueA, valueB, caseSensitive);
                };
            } catch (Exception e) {
                // Fallback to string comparison if type conversion fails
                return compareStrings(valueA, valueB, caseSensitive);
            }
        };
    }

    private int compareNumbers(Object a, Object b) {
        Double numA = convertToDouble(a);
        Double numB = convertToDouble(b);
        return numA.compareTo(numB);
    }

    private Double convertToDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private int compareDates(Object a, Object b) {
        // This is a simplified date comparison - you might want to add more sophisticated date parsing
        return a.toString().compareTo(b.toString());
    }

    private int compareBooleans(Object a, Object b) {
        Boolean boolA = convertToBoolean(a);
        Boolean boolB = convertToBoolean(b);
        return boolA.compareTo(boolB);
    }

    private Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private int compareStrings(Object a, Object b, boolean caseSensitive) {
        String strA = a.toString();
        String strB = b.toString();

        if (caseSensitive) {
            return strA.compareTo(strB);
        } else {
            return strA.compareToIgnoreCase(strB);
        }
    }
}
