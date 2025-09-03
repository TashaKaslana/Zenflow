package org.phong.zenflow.core.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapUtils {

    public static Map<String, Object> flattenMap(Map<String, Object> map) {
        Map<String, Object> flatMap = new LinkedHashMap<>();
        flattenMapHelper("", map, flatMap);
        return flatMap;
    }

    private static void flattenMapHelper(String prefix, Map<String, Object> source, Map<String, Object> dest) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String newKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                flattenMapHelper(newKey, nestedMap, dest);
            } else {
                dest.put(newKey, entry.getValue());
            }
        }
    }
}
