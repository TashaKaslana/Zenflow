package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.OutputUsage;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
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
     * Validates template types against schema at compile time for known types,
     * while allowing 'any' types for runtime flexibility
     */
    public List<ValidationError> validateTemplateType(
            String nodeKey,
            String templateString,
            Object nodeInputConfig,
            Map<String, OutputUsage> nodeConsumers,
            Set<String> templates) {

        List<ValidationError> errors = new ArrayList<>();

        try {
            JSONObject schema = schemaRegistry.getSchemaByTemplateString(templateString);
            if (schema == null || nodeConsumers == null || nodeInputConfig == null) {
                log.debug("Skipping validation for node {} - missing schema or configuration", nodeKey);
                return errors;
            }

            if (!schema.has("properties") || !schema.getJSONObject("properties").has("input")) {
                log.debug("No input schema found for node {}", nodeKey);
                return errors;
            }

            JSONObject inputSchema = schema.getJSONObject("properties").getJSONObject("input");
            if (!inputSchema.has("properties")) {
                return errors;
            }

            JSONObject inputProperties = inputSchema.getJSONObject("properties");
            JSONObject inputData = new JSONObject(nodeInputConfig.toString());

            log.debug("Validating {} templates for node {}", templates.size(), nodeKey);

            // Validate each template against its schema field
            for (String template : templates) {
                validateSingleTemplate(nodeKey, template, inputData, inputProperties,
                        nodeConsumers, errors);
            }

        } catch (Exception e) {
            log.error("Error validating template types for node {}: {}", nodeKey, e.getMessage(), e);
            errors.add(ValidationError.builder()
                    .nodeKey(nodeKey)
                    .errorType("definition")
                    .errorCode(ValidationErrorCode.TEMPLATE_TYPE_VALIDATION_ERROR)
                    .path(nodeKey + ".config")
                    .message("Error validating template types: " + e.getMessage())
                    .build());
        }

        return errors;
    }

    private void validateSingleTemplate(String nodeKey, String template,
                                        JSONObject inputData, JSONObject inputProperties,
                                        Map<String, OutputUsage> nodeConsumers,
                                        List<ValidationError> errors) {

        // Find which input field contains this template
        FieldLocation fieldLocation = findFieldContainingTemplate(inputData, template);
        if (fieldLocation == null) {
            log.debug("Template {} not found in input data for node {}", template, nodeKey);
            return;
        }

        // Get the schema for this field
        JSONObject fieldSchema = getSchemaForPath(inputProperties, fieldLocation.path);
        if (fieldSchema == null) {
            log.debug("No schema found for field path {} in node {}", fieldLocation.path, nodeKey);
            return;
        }

        // Get expected type from schema
        String expectedType = getSchemaType(fieldSchema);

        // Get actual type from nodeConsumers
        OutputUsage consumer = nodeConsumers.get(template);
        if (consumer != null) {
            String actualType = consumer.getType();

            // Only validate if we have a known type (not 'any')
            if (actualType != null && !actualType.equals("any") &&
                    !isTypeCompatible(expectedType, actualType)) {

                errors.add(ValidationError.builder()
                        .nodeKey(nodeKey)
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.TYPE_MISMATCH)
                        .path(nodeKey + ".config.input." + fieldLocation.topLevelField)
                        .message(String.format(
                                "Type mismatch in field '%s': expects '%s' but template '%s' provides '%s'",
                                fieldLocation.topLevelField, expectedType, template, actualType
                        ))
                        .template("{{" + template + "}}")
                        .value(actualType)
                        .expectedType(expectedType)
                        .schemaPath("$.nodes[?(@.key=='" + nodeKey + "')].config.input." + fieldLocation.topLevelField)
                        .build());

                log.debug("Type mismatch found: {} expects {} but {} provides {}",
                        fieldLocation.topLevelField, expectedType, template, actualType);
            } else {
                log.debug("Template {} has type {} which is compatible with expected {} or is flexible",
                        template, actualType, expectedType);
            }
        } else {
            log.debug("No consumer found for template {} in node {}", template, nodeKey);
        }
    }

    /**
     * Recursively finds which input field contains the given template
     */
    private FieldLocation findFieldContainingTemplate(JSONObject inputData, String template) {
        return findFieldContainingTemplateRecursive(inputData, template, "", "");
    }

    private FieldLocation findFieldContainingTemplateRecursive(JSONObject obj, String template,
                                                               String currentPath, String topLevelField) {
        for (String key : obj.keySet()) {
            Object value = obj.get(key);
            String fieldPath = currentPath.isEmpty() ? key : currentPath + "." + key;
            String topLevel = topLevelField.isEmpty() ? key : topLevelField;

            if (value instanceof JSONObject) {
                FieldLocation result = findFieldContainingTemplateRecursive(
                        (JSONObject) value, template, fieldPath, topLevel);
                if (result != null) return result;
            } else if (value instanceof JSONArray array) {
                for (int i = 0; i < array.length(); i++) {
                    Object arrayItem = array.get(i);
                    if (arrayItem instanceof JSONObject) {
                        FieldLocation result = findFieldContainingTemplateRecursive(
                                (JSONObject) arrayItem, template, fieldPath + "[" + i + "]", topLevel);
                        if (result != null) return result;
                    } else if (arrayItem instanceof String) {
                        Set<String> templatesInValue = TemplateEngine.extractRefs(arrayItem.toString());
                        if (templatesInValue.contains(template)) {
                            return new FieldLocation(fieldPath, topLevel);
                        }
                    }
                }
            } else if (value instanceof String) {
                Set<String> templatesInValue = TemplateEngine.extractRefs(value.toString());
                if (templatesInValue.contains(template)) {
                    return new FieldLocation(fieldPath, topLevel);
                }
            }
        }
        return null;
    }

    /**
     * Gets the schema for a specific field path
     */
    private JSONObject getSchemaForPath(JSONObject inputProperties, String path) {
        String[] parts = path.split("\\.");
        JSONObject current = inputProperties;

        for (String part : parts) {
            // Skip array indices
            if (part.matches(".*\\[\\d+]")) {
                part = part.replaceAll("\\[\\d+]", "");
            }

            if (current.has(part)) {
                JSONObject property = current.getJSONObject(part);

                // If this is the last part, return the property
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
                    return property; // Can't navigate further
                }
            } else {
                return null; // Path not found
            }
        }

        return current;
    }

    private String getSchemaType(JSONObject propertySchema) {
        if (propertySchema.has("type")) {
            return propertySchema.getString("type");
        }
        if (propertySchema.has("enum")) {
            return "string"; // Enums are typically strings
        }
        return "any"; // Default to any if no type specified
    }

    /**
     * Checks if types are compatible for compile-time validation
     */
    private boolean isTypeCompatible(String expectedType, String actualType) {
        // 'any' type is always compatible (runtime flexibility)
        if (expectedType.equals("any") || actualType.equals("any") || actualType.equals("mixed")) {
            return true;
        }

        // Handle array types
        if (actualType.startsWith("array:")) {
            return expectedType.equals("array");
        }

        // Special case for numbers - integer can be used where number is expected
        if (expectedType.equals("number") && actualType.equals("integer")) {
            return true;
        }

        // Direct type match
        return expectedType.equals(actualType);
    }

    /**
     * Helper class to store field location information
     */
    private record FieldLocation(String path, String topLevelField) {
    }
}
