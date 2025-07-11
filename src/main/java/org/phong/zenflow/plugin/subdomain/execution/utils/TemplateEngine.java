package org.phong.zenflow.plugin.subdomain.execution.utils;

import org.phong.zenflow.core.utils.ObjectConversion;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

public class TemplateEngine {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9._-]+)\\s*}}");

    public static Object resolveTemplate(Object value, Map<String, Object> context) {
        if (!(value instanceof String template)) return value;

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1); // e.g., steps.step1.userId
            Object replacement = deepGet(context, key);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement != null ? replacement.toString() : ""));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static Object deepGet(Map<String, Object> map, String dottedKey) {
        String[] keys = dottedKey.split("\\.");
        Object current = map;
        for (String k : keys) {
            if (!(current instanceof Map)) return null;
            current = ((Map<?, ?>) current).get(k);
        }
        return current;
    }

    // Recursively resolve config map
    public static Map<String, Object> resolveAll(Map<String, Object> input, Map<String, Object> context) {
        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                resolved.put(entry.getKey(), resolveAll(ObjectConversion.convertObjectToMap(value), context));
            } else {
                resolved.put(entry.getKey(), resolveTemplate(value, context));
            }
        }
        return resolved;
    }
}
