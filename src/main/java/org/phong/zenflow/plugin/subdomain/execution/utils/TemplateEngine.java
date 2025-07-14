package org.phong.zenflow.plugin.subdomain.execution.utils;

import org.phong.zenflow.core.utils.ObjectConversion;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.*;

public class TemplateEngine {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9._-]+(?:\\(.*?\\))?)\\s*}}");
    private static final Map<String, Function<List<String>, Object>> functionRegistry = new HashMap<>();
    static {
        functionRegistry.put("uuid", args -> UUID.randomUUID().toString());
        functionRegistry.put("now", args -> Instant.now().toString());
        functionRegistry.put("env", args -> System.getenv(args.getFirst()));
        functionRegistry.put("upper", args -> args.getFirst().toUpperCase());
        functionRegistry.put("lower", args -> args.getFirst().toLowerCase());
        functionRegistry.put("slug", args -> args.getFirst().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
    }

    public static Object resolveTemplate(Object value, Map<String, Object> context) {
        if (!(value instanceof String template)) return value;

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String expr = matcher.group(1); // e.g., uuid(), user.name

            Object replacement;
            if (expr.matches("[a-zA-Z0-9._-]+\\(.*\\)")) {
                replacement = evaluateFunctionWithArgs(expr);
            } else {
                replacement = deepGet(context, expr);
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    replacement != null ? replacement.toString() : ""
            ));
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

    private static Object evaluateFunctionWithArgs(String expr) {
        int open = expr.indexOf('(');
        int close = expr.lastIndexOf(')');
        if (open < 0 || close < 0 || close <= open) {
            throw new IllegalArgumentException("Malformed function expression: " + expr);
        }

        String funcName = expr.substring(0, open);
        String rawArgs = expr.substring(open + 1, close);
        List<String> args = Arrays.stream(rawArgs.split("\\s*,\\s*")).toList();

        Function<List<String>, Object> fn = functionRegistry.get(funcName.toLowerCase());
        if (fn == null) throw new IllegalArgumentException("Unknown function: " + funcName);

        return fn.apply(args);
    }


    // Recursively resolve config map
    public static Map<String, Object> resolveAll(Map<String, Object> input, Map<String, Object> context) {
        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                resolved.put(entry.getKey(), resolveAll(ObjectConversion.convertObjectToMap(value), context));
            } else if (value instanceof List<?> list) {
                List<Object> resolvedList = list.stream()
                        .map(item -> item instanceof Map
                                ? resolveAll(ObjectConversion.convertObjectToMap(item), context)
                                : resolveTemplate(item, context))
                        .toList();
                resolved.put(entry.getKey(), resolvedList);
            }
            else {
                resolved.put(entry.getKey(), resolveTemplate(value, context));
            }
        }
        return resolved;
    }
}
