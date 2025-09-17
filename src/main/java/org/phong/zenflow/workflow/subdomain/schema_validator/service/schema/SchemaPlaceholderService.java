package org.phong.zenflow.workflow.subdomain.schema_validator.service.schema;

import lombok.AllArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.springframework.stereotype.Component;

import net.datafaker.Faker;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Component
@AllArgsConstructor
public class SchemaPlaceholderService {
    private final SchemaTypeResolver schemaTypeResolver;
    private final TemplateService templateService;
    private final Faker faker = new Faker();

    /**
     * Replaces template fields with type-appropriate placeholder values instead of removing them.
     * This prevents "required key not found" errors during definition-phase validation.
     *
     * @param jsonObject The JSON object to process
     * @param schemaProperties The schema properties to determine appropriate placeholder types
     * @return A new JSON object with template fields replaced by type-appropriate placeholders
     */
    public JSONObject replaceTemplateFieldsWithPlaceholders(JSONObject jsonObject, JSONObject schemaProperties) {
        JSONObject processedObject = new JSONObject();

        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);

            switch (value) {
                case JSONObject object -> {
                    // Get nested schema if available
                    JSONObject nestedSchema = null;
                    if (schemaProperties != null && schemaProperties.has(key) &&
                            schemaProperties.getJSONObject(key).has("properties")) {
                        nestedSchema = schemaProperties.getJSONObject(key).getJSONObject("properties");
                    }

                    // Recursively process nested objects
                    JSONObject processedNested = replaceTemplateFieldsWithPlaceholders(object, nestedSchema);
                    processedObject.put(key, processedNested);
                }
                case JSONArray array -> {
                    // Process arrays - replace template values with appropriate placeholders
                    JSONArray processedArray = new JSONArray();

                    for (int i = 0; i < array.length(); i++) {
                        Object arrayItem = array.get(i);
                        if (arrayItem instanceof JSONObject) {
                            // For array items, we can't easily determine the schema, so pass null
                            JSONObject processedItem = replaceTemplateFieldsWithPlaceholders((JSONObject) arrayItem, null);
                            processedArray.put(processedItem);
                        } else if (arrayItem instanceof String && templateService.isTemplate((String) arrayItem)) {
                            // Replace template strings in arrays with string placeholders
                            processedArray.put("__TEMPLATE_PLACEHOLDER__");
                        } else {
                            processedArray.put(arrayItem);
                        }
                    }

                    processedObject.put(key, processedArray);
                }
                case String s when templateService.isTemplate(s) -> {
                    // Replace template strings with appropriate type placeholders based on schema
                    Object placeholder = getPlaceholderForSchemaField(key, schemaProperties);
                    processedObject.put(key, placeholder);
                }
                case null, default ->
                    // Keep non-template values as-is
                        processedObject.put(key, value);
            }
        }

        return processedObject;
    }

    /**
     * Gets an appropriate placeholder value for a schema field based on its expected type and constraints
     */
    private Object getPlaceholderForSchemaField(String fieldName, JSONObject schemaProperties) {
        if (schemaProperties == null || !schemaProperties.has(fieldName)) {
            return "__TEMPLATE_PLACEHOLDER__"; // Default string placeholder
        }

        JSONObject fieldSchema = schemaProperties.getJSONObject(fieldName);
        String expectedType = schemaTypeResolver.getSchemaType(fieldSchema);

        return switch (expectedType) {
            case "integer" -> generateIntegerPlaceholder(fieldSchema);
            case "number" -> generateNumberPlaceholder(fieldSchema);
            case "boolean" -> false;
            case "array" -> generateArrayPlaceholder(fieldSchema);
            case "object" -> new JSONObject();
            default -> generateStringPlaceholder(fieldName, fieldSchema);
        };
    }

    /**
     * Generates a string placeholder that satisfies schema constraints
     */
    private String generateStringPlaceholder(String fieldName, JSONObject fieldSchema) {
        if (fieldSchema.has("enum")) {
            JSONArray enumValues = fieldSchema.optJSONArray("enum");
            if (enumValues != null && !enumValues.isEmpty()) {
                Object first = enumValues.get(0);
                if (first != null) {
                    return first.toString();
                }
            }
        }

        String format = fieldSchema.optString("format", null);
        if (format != null && !format.isBlank()) {
            String formatted = generateFormattedPlaceholder(format, fieldSchema);
            if (formatted != null) {
                return formatted;
            }
        }

        if (fieldName != null) {
            String heuristic = generateHeuristicPlaceholder(fieldName, fieldSchema);
            if (heuristic != null) {
                return heuristic;
            }
        }

        String pattern = fieldSchema.optString("pattern", null);
        if (pattern != null && !pattern.isEmpty()) {
            String patternPlaceholder = generatePatternPlaceholder(pattern, fieldSchema);
            if (patternPlaceholder != null) {
                return patternPlaceholder;
            }
        }

        int minLength = fieldSchema.optInt("minLength", 0);
        int maxLength = fieldSchema.optInt("maxLength", Math.max(50, minLength + 10)); // Default to reasonable max

        // Ensure minLength is satisfied
        int targetLength = Math.max(minLength, 24); // At least 24 chars for "__TEMPLATE_PLACEHOLDER__"

        // If maxLength is specified and less than our target, respect it
        if (fieldSchema.has("maxLength") && maxLength < targetLength) {
            targetLength = maxLength;
        }

        // Build placeholder string to meet length requirements
        StringBuilder placeholder = new StringBuilder("__TEMPLATE_PLACEHOLDER");

        // Pad with underscores if needed to meet minLength
        while (placeholder.length() < targetLength) {
            placeholder.append("_");
        }

        // Add closing "__"
        placeholder.append("__");

        // Trim if we exceeded maxLength
        if (fieldSchema.has("maxLength") && placeholder.length() > maxLength) {
            return placeholder.substring(0, maxLength);
        }

        return placeholder.toString();
    }


    private String generateFormattedPlaceholder(String format, JSONObject fieldSchema) {
        String value = switch (format.toLowerCase()) {
            case "email", "idn-email" -> faker.internet().emailAddress();
            case "hostname" -> faker.internet().domainName();
            case "ipv4" -> faker.internet().ipV4Address();
            case "ipv6" -> faker.internet().ipV6Address();
            case "url", "uri" -> faker.internet().url();
            case "uuid" -> faker.internet().uuid();
            case "date-time" -> OffsetDateTime.now().withNano(0).toString();
            case "date" -> LocalDate.now().toString();
            case "time" -> LocalTime.now().withNano(0).toString();
            case "phone", "phone-number", "tel" -> faker.phoneNumber().phoneNumber();
            default -> null;
        };
        if (value == null) {
            return null;
        }
        return coerceToLength(value, fieldSchema);
    }

    private String generateHeuristicPlaceholder(String fieldName, JSONObject fieldSchema) {
        String lower = fieldName.toLowerCase();
        if (lower.contains("email")) {
            return coerceToLength(faker.internet().emailAddress(), fieldSchema);
        }
        if (lower.contains("phone") || lower.contains("tel")) {
            return coerceToLength(faker.phoneNumber().phoneNumber(), fieldSchema);
        }
        if (lower.contains("url") || lower.contains("uri")) {
            return coerceToLength(faker.internet().url(), fieldSchema);
        }
        if (lower.contains("ip")) {
            return coerceToLength(faker.internet().ipV4Address(), fieldSchema);
        }
        if (lower.contains("uuid")) {
            return coerceToLength(faker.internet().uuid(), fieldSchema);
        }
        if (lower.endsWith("id") || lower.contains("_id")) {
            int length = resolveLength(fieldSchema, 18);
            String digits = faker.number().digits(Math.max(length, 1));
            return coerceToLength(digits, fieldSchema);
        }
        if (lower.contains("name")) {
            return coerceToLength(faker.credentials().username(), fieldSchema);
        }
        return null;
    }

    private String generatePatternPlaceholder(String pattern, JSONObject fieldSchema) {
        String sanitized = stripAnchors(pattern);
        if (sanitized.isEmpty()) {
            return null;
        }
        try {
            String candidate = faker.regexify(sanitized);
            if (candidate == null || candidate.isEmpty()) {
                return null;
            }
            return coerceToLength(candidate, fieldSchema);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String stripAnchors(String pattern) {
        String trimmed = pattern;
        if (trimmed.startsWith("^")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("$")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String coerceToLength(String value, JSONObject fieldSchema) {
        String base = value == null || value.isEmpty() ? "a" : value;
        int min = Math.max(0, fieldSchema.optInt("minLength", 0));
        int max = fieldSchema.has("maxLength") ? Math.max(min, fieldSchema.optInt("maxLength")) : Integer.MAX_VALUE;

        StringBuilder builder = new StringBuilder(base);
        if (builder.length() < min) {
            char padChar = builder.charAt(builder.length() - 1);
            while (builder.length() < min) {
                builder.append(padChar);
            }
        }

        if (builder.length() > max) {
            builder.setLength(max);
        }

        return builder.toString();
    }

    private int resolveLength(JSONObject fieldSchema, int defaultLength) {
        int min = Math.max(0, fieldSchema.optInt("minLength", 0));
        int max = fieldSchema.has("maxLength") ? fieldSchema.optInt("maxLength") : Integer.MAX_VALUE;
        int length = Math.max(defaultLength, min);
        if (max != Integer.MAX_VALUE) {
            length = Math.min(length, Math.max(min, max));
        }

        return length;
    }
    /**
     * Generates an integer placeholder that satisfies schema constraints
     */
    private int generateIntegerPlaceholder(JSONObject fieldSchema) {
        int minimum = fieldSchema.optInt("minimum", 0);
        int maximum = fieldSchema.optInt("maximum", Integer.MAX_VALUE);

        // Return a value within the specified range
        if (fieldSchema.has("minimum") && fieldSchema.has("maximum")) {
            return Math.min(minimum + 1, maximum); // Prefer minimum + 1 if possible
        } else if (fieldSchema.has("minimum")) {
            return minimum;
        } else if (fieldSchema.has("maximum")) {
            return Math.min(0, maximum);
        }

        return 0; // Default
    }

    /**
     * Generates a number placeholder that satisfies schema constraints
     */
    private double generateNumberPlaceholder(JSONObject fieldSchema) {
        double minimum = fieldSchema.optDouble("minimum", 0.0);
        double maximum = fieldSchema.optDouble("maximum", Double.MAX_VALUE);

        // Return a value within the specified range
        if (fieldSchema.has("minimum") && fieldSchema.has("maximum")) {
            return Math.min(minimum + 1.0, maximum); // Prefer minimum + 1 if possible
        } else if (fieldSchema.has("minimum")) {
            return minimum;
        } else if (fieldSchema.has("maximum")) {
            return Math.min(0.0, maximum);
        }

        return 0.0; // Default
    }

    /**
     * Generates an array placeholder that satisfies schema constraints
     */
    private JSONArray generateArrayPlaceholder(JSONObject fieldSchema) {
        JSONArray array = new JSONArray();

        int minItems = fieldSchema.optInt("minItems", 0);

        // Add placeholder items to meet minItems requirement
        for (int i = 0; i < minItems; i++) {
            // Add appropriate placeholder based on array item schema
            if (fieldSchema.has("items")) {
                Object itemSchema = fieldSchema.get("items");
                if (itemSchema instanceof JSONObject) {
                    String itemType = schemaTypeResolver.getSchemaType((JSONObject) itemSchema);
                    switch (itemType) {
                        case "string" -> array.put("__TEMPLATE_ITEM__");
                        case "integer" -> array.put(0);
                        case "number" -> array.put(0.0);
                        case "boolean" -> array.put(false);
                        case "object" -> array.put(new JSONObject());
                        default -> array.put("__TEMPLATE_ITEM__");
                    }
                } else {
                    array.put("__TEMPLATE_ITEM__");
                }
            } else {
                array.put("__TEMPLATE_ITEM__");
            }
        }

        return array;
    }
}
