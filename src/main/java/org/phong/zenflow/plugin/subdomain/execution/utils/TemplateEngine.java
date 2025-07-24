package org.phong.zenflow.plugin.subdomain.execution.utils;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.*;

@Slf4j
public class TemplateEngine {

    // Updated pattern to handle both simple references and function calls
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

    /**
     * Extracts all references from a template string.
     * For example, "Hello {{user.name}}, your order {{order.id}} is ready" would return ["user.name", "order.id"]
     *
     * @param template The template string containing references
     * @return List of references found in the template
     */
    public static List<String> extractRefs(String template) {
        if (template == null) {
            return new ArrayList<>();
        }

        List<String> refs = new ArrayList<>();
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);

        while (matcher.find()) {
            refs.add(matcher.group(1).trim());
        }

        return refs;
    }

    /**
     * Determines if a string contains any template references.
     *
     * @param value The string to check
     * @return true if the string contains template references, false otherwise
     */
    public static boolean isTemplate(String value) {
        return value != null && TEMPLATE_PATTERN.matcher(value).find();
    }

    /**
     * Resolves a template string by replacing references with their values from the context.
     *
     * @param value The template string to resolve
     * @param context The context containing values for references
     * @return The resolved template
     */
    public static Object resolveTemplate(Object value, Map<String, Object> context) {
        if (!(value instanceof String template)) return value;

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String expr = matcher.group(1); // e.g., uuid(), user.name, secrets.API_KEY

            Object replacement;
            if (expr.matches("[a-zA-Z0-9._-]+\\(.*\\)")) {
                replacement = evaluateFunction(expr);
            } else {
                // Check if this is a secret reference
                if (expr.startsWith("secrets.")) {
                    String secretName = expr.substring("secrets.".length());
                    replacement = resolveSecret(context, secretName);
                } else {
                    replacement = deepGet(context, expr);
                }
            }

            // Handle null values gracefully
            String replacementString;
            if (replacement != null) {
                replacementString = replacement.toString();
            } else {
                // If the entire template is just one reference that resolves to null,
                // keep the original template syntax for better debugging
                if (template.trim().equals("{{" + expr + "}}")) {
                    log.warn("Template reference '{}' resolved to null, keeping original template", expr);
                } else {
                    // For complex templates with null references, return the original template
                    // This prevents malformed expressions like "null > 200" from being created
                    log.warn("Template reference '{}' in complex template '{}' resolved to null, returning original template", expr, template);
                }
                return template; // Return original template for single null references
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacementString));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Resolves a secret reference from the context.
     *
     * @param context The context containing the secrets map
     * @param secretName The name of the secret to resolve
     * @return The secret value, or null if not found
     */
    @SuppressWarnings("unchecked")
    private static Object resolveSecret(Map<String, Object> context, String secretName) {
        if (context == null) return null;

        Object secretsObj = context.get("secrets");
        if (secretsObj == null) return null;

        // Handle both Map<String, String> and Map<String, Object> formats
        if (secretsObj instanceof Map) {
            return ((Map<String, ?>) secretsObj).get(secretName);
        }

        return null;
    }

    /**
     * Retrieves a nested value from a map using dot notation.
     * For example, "user.address.city" would retrieve context.get("user").get("address").get("city")
     *
     * @param map The map to search in
     * @param dottedKey The key in dot notation
     * @return The value found, or null if not found
     */
    private static Object deepGet(Map<String, Object> map, String dottedKey) {
        String[] keys = dottedKey.split("\\.");
        Object current = map;
        for (String k : keys) {
            if (!(current instanceof Map)) return null;
            current = ((Map<?, ?>) current).get(k);
        }
        return current;
    }

    /**
     * Evaluates a function expression with arguments.
     * For example, "uuid()" would call the uuid function.
     *
     * @param expr The function expression
     * @return The result of the function call
     */
    public static Object evaluateFunction(String expr) {
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

    /**
     * Recursively resolves all templates in a map.
     *
     * @param input The map containing templates
     * @param context The context containing values for references
     * @return A new map with all templates resolved
     */
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
