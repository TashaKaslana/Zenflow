package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.plugin.subdomain.node.utils.SchemaRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.OutputUsage;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@AllArgsConstructor
public class SchemaTemplateValidationService {
    private final SchemaRegistry schemaRegistry;

    /**
     * Validates that template references in a node's input have compatible types with
     * the types defined in nodeConsumers.
     *
     * @param nodeKey        The key of the current node being validated
     * @param templateString The ID of the node type (used to retrieve schema from SchemaRegistry)
     * @param nodeConsumers  Map of output fields and their types from the workflow context
     * @param templates      Set of template references which found in the node's input
     * @return List of validation errors for type mismatches
     */
    public List<ValidationError> validateTemplateType(
            String nodeKey,
            String templateString,
            Map<String, OutputUsage> nodeConsumers,
            Set<String> templates) {
        List<ValidationError> errors = new ArrayList<>();

        try {
            // Get the schema using SchemaRegistry
            JSONObject schema = schemaRegistry.getSchemaByTemplateString(templateString);
            if (schema == null || nodeConsumers == null || !schema.has("properties") || !schema.getJSONObject("properties").has("input")) {
                return errors;
            }

            // Get the input schema
            JSONObject inputSchema = schema.getJSONObject("properties").getJSONObject("input");
            if (!inputSchema.has("properties")) {
                return errors;
            }

            JSONObject inputProperties = inputSchema.getJSONObject("properties");

            // For each template reference, check if the type is compatible
            for (String template : templates) {
                // Check if this is a path to a field in the node's input schema
                String[] parts = template.split("\\.");
                if (parts.length < 3) {
                    continue; // Not a node output reference
                }

                // Find the property in the input schema that contains this template
                for (String inputField : inputProperties.keySet()) {
                    if (getTemplateForProperty(inputProperties.getJSONObject(inputField)).contains(template)) {
                        // Get the expected type from the input schema
                        String expectedType = getSchemaType(inputProperties.getJSONObject(inputField));

                        // Get the actual type from the nodeConsumer map
                        OutputUsage consumer = nodeConsumers.get(template);
                        String actualType = consumer.getType();
                        if (actualType != null && !isTypeCompatible(expectedType, actualType)) {
                            errors.add(ValidationError.builder()
                                    .type("TYPE_MISMATCH")
                                    .path(nodeKey + ".input." + inputField)
                                    .message("Type mismatch: Field expects '" + expectedType +
                                            "' but template '" + template + "' provides '" + actualType + "'")
                                    .template("{{" + template + "}}")
                                    .value(actualType)
                                    .expectedType(expectedType)
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            errors.add(ValidationError.builder()
                    .type("TYPE_VALIDATION_ERROR")
                    .path(nodeKey)
                    .message("Error validating template types: " + e.getMessage())
                    .build());
        }

        return errors;
    }

    /**
     * Gets the schema type from a JSON schema property definition
     *
     * @param propertySchema The schema for a property
     * @return The type as a string
     */
    private String getSchemaType(JSONObject propertySchema) {
        if (propertySchema.has("type")) {
            return propertySchema.getString("type");
        }
        return "any"; // Default to any if no type specified
    }

    /**
     * Gets the templates referenced in a property
     *
     * @param propertySchema The schema for a property
     * @return Set of template references
     */
    private Set<String> getTemplateForProperty(JSONObject propertySchema) {
        // Extract default value or examples if they exist
        Object defaultValue = propertySchema.opt("default");
        return TemplateEngine.extractRefs(defaultValue != null ? defaultValue.toString() : "");
    }

    /**
     * Checks if the actual type is compatible with the expected type
     *
     * @param expectedType The type expected by the schema
     * @param actualType   The actual type from nodeConsumer
     * @return true if types are compatible
     */
    private boolean isTypeCompatible(String expectedType, String actualType) {
        // Handle array types
        if (actualType.startsWith("array:")) {
            return expectedType.equals("array") || expectedType.equals("any");
        }

        // Special case for numbers
        if ((expectedType.equals("number") || expectedType.equals("integer")) &&
                (actualType.equals("number") || actualType.equals("integer"))) {
            return true;
        }

        // Direct match or any type
        return expectedType.equals(actualType) || expectedType.equals("any") ||
                actualType.equals("any") || actualType.equals("mixed");
    }
}
