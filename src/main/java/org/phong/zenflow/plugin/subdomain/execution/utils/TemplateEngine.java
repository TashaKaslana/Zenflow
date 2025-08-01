package org.phong.zenflow.plugin.subdomain.execution.utils;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.*;

@Slf4j
public class TemplateEngine {

    // Updated pattern to handle both simple references, function calls, and default values
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_.\\-]+(?:\\([^)]*\\))?(?::[^}]*)?)}}");
    // Pattern to extract reference and default value
    private static final Pattern REF_WITH_DEFAULT_PATTERN = Pattern.compile("^([a-zA-Z0-9_.\\-]+)(?::(.*))?$");
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
     * For templates with default values like {{user.name:John}}, extracts just the reference part (user.name)
     * For example, "Hello {{user.name:Guest}}, your order {{order.id}} is ready" would return ["user.name", "order.id"]
     *
     * @param template The template string containing references
     * @return Set of references found in the template
     */
    public static Set<String> extractRefs(String template) {
        if (template == null) {
            return new LinkedHashSet<>();
        }

        Set<String> refs = new LinkedHashSet<>();
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);

        while (matcher.find()) {
            String fullExpression = matcher.group(1).trim();

            // Extract just the reference part (before the colon if present)
            Matcher refMatcher = REF_WITH_DEFAULT_PATTERN.matcher(fullExpression);
            if (refMatcher.matches()) {
                String reference = refMatcher.group(1);
                // Only add if it's not a function call
                if (!reference.matches("[a-zA-Z0-9._-]+\\(.*\\)")) {
                    refs.add(reference);
                }
            }
        }

        return refs;
    }

    public static Set<String> extractRefs(Object value) {
        if (value instanceof String template) {
            return extractRefs(template);
        } else if (value instanceof Map<?, ?> map) {
            Set<String> refs = new LinkedHashSet<>();
            for (Object v : map.values()) {
                refs.addAll(extractRefs(v));
            }
            return refs;
        } else if (value instanceof List<?> list) {
            Set<String> refs = new LinkedHashSet<>();
            for (Object item : list) {
                refs.addAll(extractRefs(item));
            }
            return refs;
        } else if (value != null) {
            // Handles custom objects by attempting to convert them to a Map using ObjectConversion.
            try {
                // Convert the object to a Map
                Map<?, ?> map = ObjectConversion.convertObjectToMap(value);
                return extractRefs(map);
            } catch (Exception e) {
                // Fallback: extract references from the object's string representation.
                return extractRefs(value.toString());
            }
        }
        return new LinkedHashSet<>();
    }

    public static Set<String> extractFullRefs(String template) {
        Set<String> refs = new LinkedHashSet<>();
        if (template == null) return refs;

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        while (matcher.find()) {
            refs.add(matcher.group(1).trim()); // includes ":0"
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
     * Supports default values using syntax like {{ref:defaultValue}}
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
            String fullExpression = matcher.group(1); // e.g., uuid(), user.name:Guest, node.output.index:0

            Object replacement;

            // Check if this is a function call
            if (fullExpression.matches("[a-zA-Z0-9._-]+\\(.*\\)")) {
                replacement = evaluateFunction(fullExpression);
            } else {
                // Parse reference and default value
                Matcher refMatcher = REF_WITH_DEFAULT_PATTERN.matcher(fullExpression);
                if (refMatcher.matches()) {
                    String reference = refMatcher.group(1);
                    String defaultValue = refMatcher.group(2); // null if no default provided

                    // Resolve the reference
                    if (reference.startsWith("secrets.")) {
                        String secretName = reference.substring("secrets.".length());
                        replacement = resolveSecret(context, secretName);
                    } else {
                        replacement = deepGet(context, reference);
                    }

                    // Use default value if reference resolved to null
                    if (replacement == null && defaultValue != null) {
                        replacement = parseDefaultValue(defaultValue);
                        log.debug("Using default value '{}' for reference '{}'", defaultValue, reference);
                    }
                } else {
                    // Fallback for malformed expressions
                    replacement = null;
                }
            }

            // Handle null values gracefully
            String replacementString;
            if (replacement != null) {
                replacementString = replacement.toString();
            } else {
                // If the entire template is just one reference that resolves to null,
                // keep the original template syntax for better debugging
                if (template.trim().matches("\\{\\{[^}]+}}")) {
                    log.warn("Template reference '{}' resolved to null, keeping original template", fullExpression);
                } else {
                    // For complex templates with null references, return the original template
                    // This prevents malformed expressions like "null > 200" from being created
                    log.warn("Template reference '{}' in complex template '{}' resolved to null, returning original template", fullExpression, template);
                }
                return template; // Return original template for single null references
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacementString));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Extract default value from a template string like {{ref:defaultValue}}
     *
     * @param template The template string
     * @return The parsed default value, or null if no default is present
     */
    public static Object extractDefaultValue(String template) {
        if (!isTemplate(template)) {
            return null;
        }

        // Extract the full expression from the template
        String content = template.trim();
        if (content.startsWith("{{") && content.endsWith("}}")) {
            String expression = content.substring(2, content.length() - 2).trim();

            // Check if it has a default value (contains colon)
            int colonIndex = expression.indexOf(':');
            if (colonIndex > 0) {
                String defaultValueStr = expression.substring(colonIndex + 1).trim();
                return parseDefaultValue(defaultValueStr);
            }
        }

        return null;
    }

    /**
     * Parse a default value string and convert it to the appropriate type
     *
     * @param defaultValue The default value as a string
     * @return The parsed default value with appropriate type
     */
    private static Object parseDefaultValue(String defaultValue) {
        if (defaultValue == null || defaultValue.trim().isEmpty()) {
            return null;
        }

        String trimmed = defaultValue.trim();

        // Try to parse as integer
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
        }

        // Try to parse as double
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException ignored) {
        }

        // Try to parse as boolean
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return Boolean.parseBoolean(trimmed);
        }

        // Return as string (remove quotes if present)
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }

        return trimmed;
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
     * Extracts the referenced node from a template string.<br>
     * For example, "user.name" would return "user"
     * or "node1.output.email" would return "node1".
     *
     * @param templateExpression The template string to analyze
     * @return The referenced node name, or null if not a valid template
     */
    public static String getReferencedNode(String templateExpression, Map<String, String> aliasMap) {
        if (templateExpression == null || templateExpression.isEmpty()) {
            return null;
        }

        //if the template is an aliases, return the actual template reference node
        if (aliasMap != null && !aliasMap.isEmpty()) {
            String actualTemplate = aliasMap.get(templateExpression);
            if (actualTemplate != null) {
                templateExpression = actualTemplate.substring(2, actualTemplate.length() - 2); // Remove {{ and }}
            }
        }

        return templateExpression.split("\\.")[0];
    }
}
