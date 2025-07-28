package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class SchemaTypeResolver {

    /**
     * Determines the type from a JSON schema object, used for validation.
     * This is the more detailed version.
     *
     * @param schema The schema object for a field
     * @return The determined type as a string
     */
    public String determineSchemaType(JSONObject schema) {
        if (schema.has("type")) {
            String type = schema.getString("type");
            return switch (type) {
                case "string" -> "string";
                case "number", "integer" -> "number";
                case "boolean" -> "boolean";
                case "array" -> {
                    if (schema.has("items") && schema.getJSONObject("items").has("type")) {
                        yield "array:" + determineSchemaType(schema.getJSONObject("items"));
                    }
                    yield "array";
                }
                case "object" -> "object";
                default -> type;
            };
        }

        if (schema.has("oneOf") || schema.has("anyOf") || schema.has("allOf")) {
            return "mixed";
        }

        return "unknown";
    }

    /**
     * Gets the schema type for basic validation.
     * This is the simpler version.
     *
     * @param propertySchema The schema object for a property
     * @return The determined type as a string
     */
    public String getSchemaType(JSONObject propertySchema) {
        if (propertySchema.has("type")) {
            return propertySchema.getString("type");
        }
        if (propertySchema.has("enum")) {
            return "string"; // Enums are typically strings
        }
        return "any"; // Default to any if no type specified
    }

    /**
     * Checks if types are compatible for compile-time validation.
     *
     * @param expectedType The expected type from the schema
     * @param actualType   The actual type from the consumer
     * @return True if types are compatible, false otherwise
     */
    public boolean isTypeCompatible(String expectedType, String actualType) {
        // 'any' or 'mixed' types are always compatible for runtime flexibility
        if ("any".equals(expectedType) || "any".equals(actualType) || "mixed".equals(actualType)) {
            return true;
        }

        // Handle array types
        if (actualType.startsWith("array:")) {
            return "array".equals(expectedType);
        }

        // Special case for numbers: integer can be used where a number is expected
        if (("number".equals(expectedType) && "integer".equals(actualType)) ||
                ("integer".equals(expectedType) && "number".equals(actualType))) {
            return true;
        }

        // Direct type match
        return expectedType.equals(actualType);
    }

    /**
     * Gets the schema for a specific field path within a properties object.
     *
     * @param properties The JSON object containing properties
     * @param path       The dot-separated path to the field
     * @return The JSON schema for the specified path, or null if not found
     */
    public JSONObject getSchemaForPath(JSONObject properties, String path) {
        String[] parts = path.split("\\.");
        JSONObject current = properties;

        for (String part : parts) {
            // Skip array indices in path parts like "field[0]"
            part = part.replaceAll("\\[\\d+]", "");

            if (current.has(part)) {
                JSONObject property = current.getJSONObject(part);

                // If this is the last part, we've found the schema for the field
                if (part.equals(parts[parts.length - 1])) {
                    return property;
                }

                // Navigate deeper into object properties
                if (property.has("properties")) {
                    current = property.getJSONObject("properties");
                } else if (property.has("items") && property.getJSONObject("items").has("properties")) {
                    // Handle array of objects
                    current = property.getJSONObject("items").getJSONObject("properties");
                } else {
                    // Cannot navigate further down the path
                    return property;
                }
            } else {
                // Path does not exist in the schema
                return null;
            }
        }
        return current;
    }
}
