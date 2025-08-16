package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl.aggregation;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DistinctTransformer implements DataTransformer {

    @Override
    public String getName() {
        return "distinct";
    }

    @Override
    public Object transform(Object data, Map<String, Object> params) {
        if (!(data instanceof List<?> list)) {
            throw new DataTransformerExecutorException("Input must be a List for distinct transformer.");
        }

        // Support distinct by specific fields or entire objects
        List<String> distinctFields = extractDistinctFields(params);

        if (distinctFields.isEmpty()) {
            // Distinct by entire object
            return list.stream().distinct().collect(Collectors.toList());
        } else {
            // Distinct by specific fields
            return getDistinctByFields(list, distinctFields);
        }
    }

    private List<String> extractDistinctFields(Map<String, Object> params) {
        if (params == null) {
            return Collections.emptyList();
        }

        Object fieldsObj = params.get("fields");
        if (fieldsObj instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toList());
        } else if (fieldsObj instanceof String str) {
            return List.of(str);
        }

        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Object> getDistinctByFields(List<?> list, List<String> distinctFields) {
        Set<String> seenKeys = new HashSet<>();
        List<Object> result = new ArrayList<>();

        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                String key = createDistinctKey((Map<String, Object>) map, distinctFields);
                if (!seenKeys.contains(key)) {
                    seenKeys.add(key);
                    result.add(item);
                }
            } else {
                // For non-map items, use the item itself as key
                String key = String.valueOf(item);
                if (!seenKeys.contains(key)) {
                    seenKeys.add(key);
                    result.add(item);
                }
            }
        }

        return result;
    }

    private String createDistinctKey(Map<String, Object> item, List<String> fields) {
        return fields.stream()
                .map(field -> String.valueOf(item.get(field)))
                .collect(Collectors.joining("||"));
    }
}
