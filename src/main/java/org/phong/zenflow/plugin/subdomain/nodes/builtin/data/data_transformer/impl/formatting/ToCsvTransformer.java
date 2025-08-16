package org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.formatting;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ToCsvTransformer implements DataTransformer {
    
    @Override
    public String getName() {
        return "to_csv";
    }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        if (!(data instanceof List<?> list)) {
            throw new DataTransformerExecutorException("Input must be a List for to_csv transformer.");
        }

        if (list.isEmpty()) {
            return "";
        }

        String delimiter = (String) (params != null ? params.getOrDefault("delimiter", ",") : ",");
        String quote = (String) (params != null ? params.getOrDefault("quote", "\"") : "\"");
        boolean headers = (Boolean) (params != null ? params.getOrDefault("headers", true) : true);
        boolean includeIndex = (Boolean) (params != null ? params.getOrDefault("includeIndex", false) : false);
        List<String> columns = extractColumns(params, list);

        StringBuilder csv = new StringBuilder();

        // Add headers if requested
        if (headers) {
            List<String> headerColumns = new ArrayList<>();
            if (includeIndex) {
                headerColumns.add("index");
            }
            headerColumns.addAll(columns);
            csv.append(String.join(delimiter, headerColumns)).append("\n");
        }

        // Add data rows
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            List<String> rowValues = new ArrayList<>();

            if (includeIndex) {
                rowValues.add(String.valueOf(i));
            }

            if (item instanceof Map<?, ?> map) {
                for (String column : columns) {
                    Object value = (ObjectConversion.convertObjectToMap(map)).get(column);
                    rowValues.add(formatCsvValue(value, quote));
                }
            } else {
                // For primitive values, add as single column
                rowValues.add(formatCsvValue(item, quote));
            }

            csv.append(String.join(delimiter, rowValues)).append("\n");
        }

        return csv.toString().trim();
    }

    private List<String> extractColumns(Map<String, Object> params, List<?> list) {
        if (params != null && params.containsKey("columns")) {
            Object columnsObj = params.get("columns");
            if (columnsObj instanceof List<?> columnsList) {
                return columnsList.stream().map(Object::toString).collect(Collectors.toList());
            }
        }

        // Auto-detect columns from first item
        if (!list.isEmpty() && list.getFirst() instanceof Map<?, ?> firstItem) {
            return new ArrayList<>((ObjectConversion.convertObjectToMap(firstItem)).keySet());
        }

        // Default column name for primitive values
        return List.of("value");
    }

    private String formatCsvValue(Object value, String quote) {
        if (value == null) {
            return "";
        }

        String strValue = value.toString();

        // Escape quotes and wrap in quotes if necessary
        if (strValue.contains(",") || strValue.contains("\"") || strValue.contains("\n")) {
            strValue = strValue.replace("\"", "\"\"");
            return quote + strValue + quote;
        }

        return strValue;
    }
}
