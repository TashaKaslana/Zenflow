package org.phong.zenflow.workflow.subdomain.schema_validator.service.schema;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.OutputUsage;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
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
    private final SchemaTypeResolver schemaTypeResolver;
    private final TemplateService templateService;

    /**
     * Validates template types against schema at compile time for known types,
     * while allowing 'any' types for runtime flexibility
     */
    public List<ValidationError> validateTemplateType(
            String nodeKey,
            String templateString,
            Object nodeInputConfig,
            WorkflowMetadata metadata,
            Set<String> templates) {

        List<ValidationError> errors = new ArrayList<>();

        try {
            JSONObject schema = schemaRegistry.getSchemaByTemplateString(templateString);
            if (schema == null || metadata.nodeConsumers() == null || nodeInputConfig == null) {
                log.debug("Skipping validation for node {} - missing schema or configuration", nodeKey);
                return errors;
            }

            if (!schema.has("properties")) {
                log.debug("Schema for node {} has no 'properties' field", nodeKey);
                return errors;
            }
            JSONObject rootProperties = schema.getJSONObject("properties");

            if (!rootProperties.has("input")) {
                log.debug("No input schema found for node {}", nodeKey);
                return errors;
            }

            JSONObject inputSchema = rootProperties.getJSONObject("input");
            if (!inputSchema.has("properties")) {
                log.debug("Input schema has no properties for node {}", nodeKey);
                return errors;
            }

            JSONObject inputProperties = inputSchema.getJSONObject("properties");
            JSONObject inputData = (JSONObject) JSONObject.wrap(nodeInputConfig);

            log.debug("Validating {} templates for node {}", templates.size(), nodeKey);

            // Validate each template against its schema field
            for (String template : templates) {
                validateSingleTemplate(nodeKey, template, inputData, inputProperties,
                        metadata, errors);
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

    /**
     * Validates a single template against its schema field
     */
    private void validateSingleTemplate(String nodeKey, String template,
                                        JSONObject inputData, JSONObject inputProperties,
                                        WorkflowMetadata metadata,
                                        List<ValidationError> errors) {

        // Find which input field contains this template
        FieldLocation fieldLocation = findFieldContainingTemplate(inputData, template);
        if (fieldLocation == null) {
            log.debug("Template {} not found in input data for node {}", template, nodeKey);
            return;
        }

        // Get the actual field value to check if it's a composite string
        Object fieldValue = getFieldValue(inputData, fieldLocation.path);
        if (fieldValue == null) {
            log.debug("Field value not found for path {} in node {}", fieldLocation.path, nodeKey);
            return;
        }

        // Check if this template is part of a composite string
        boolean isCompositeString = isTemplateInCompositeString(fieldValue.toString(), template);

        // Get the schema for this field
        JSONObject fieldSchema = schemaTypeResolver.getSchemaForPath(inputProperties, fieldLocation.path);
        if (fieldSchema == null) {
            log.debug("No schema found for field path {} in node {}", fieldLocation.path, nodeKey);
            return;
        }

        // Get expected type from schema
        String expectedType = schemaTypeResolver.getSchemaType(fieldSchema);

        // Get actual type from nodeConsumers
        OutputUsage consumer = resolveTemplateToConsumer(template, metadata);
        if (consumer != null) {
            String actualType = consumer.getType();

            // Skip validation for composite strings when field expects string
            // In composite strings, templates can be of any type as they'll be converted to string
            if (isCompositeString && "string".equals(expectedType)) {
                log.debug("Skipping type validation for template {} in composite string for field {}",
                         template, fieldLocation.topLevelField);
                return;
            }

            // Only validate if we have a known type (not 'any')
            if (actualType != null && !actualType.equals("any") &&
                    !schemaTypeResolver.isTypeCompatible(expectedType, actualType)) {

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
     * Helper method to get field value from input data using dot notation path
     */
    private Object getFieldValue(JSONObject inputData, String path) {
        String[] pathParts = path.split("\\.");
        Object current = inputData;

        for (String part : pathParts) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).opt(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Checks if a template is part of a composite string (contains other text or multiple templates)
     */
    private boolean isTemplateInCompositeString(String fieldValue, String template) {
        if (fieldValue == null || template == null) {
            return false;
        }

        // Extract all templates from the field value
        Set<String> allTemplates = templateService.extractRefs(fieldValue);

        // If there's more than one template, it's definitely composite
        if (allTemplates.size() > 1) {
            return true;
        }

        // If there's only one template, check if the field value is exactly that template
        if (allTemplates.size() == 1 && allTemplates.contains(template)) {
            String expectedFullTemplate = "{{" + template + "}}";
            // If the field value is not exactly the template (has other text), it's composite
            return !fieldValue.trim().equals(expectedFullTemplate);
        }

        return false;
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
                        Set<String> templatesInValue = templateService.extractRefs(arrayItem.toString());
                        if (templatesInValue.contains(template)) {
                            return new FieldLocation(fieldPath, topLevel);
                        }
                    }
                }
            } else if (value instanceof String) {
                Set<String> templatesInValue = templateService.extractRefs(value.toString());
                if (templatesInValue.contains(template)) {
                    return new FieldLocation(fieldPath, topLevel);
                }
            }
        }
        return null;
    }

    /**
     * Resolves a template (including aliases) to its corresponding OutputUsage
     * Prioritizes aliases map for better performance
     */
    private OutputUsage resolveTemplateToConsumer(String template, WorkflowMetadata metadata) {
        Map<String, OutputUsage> nodeConsumers = metadata.nodeConsumers();
        Map<String, String> aliases = metadata.aliases();

        // Method 1: Direct lookup in nodeConsumers (fastest)
        OutputUsage directConsumer = nodeConsumers.get(template);
        if (directConsumer != null) {
            log.debug("Found direct consumer for template: {}", template);
            return directConsumer;
        }

        // Method 2: Resolve via aliases map (O(1) lookup, much faster)
        if (aliases != null && aliases.containsKey(template)) {
            String aliasValue = aliases.get(template);
            log.debug("Found alias mapping: {} -> {}", template, aliasValue);

            // Extract the actual template from alias value (e.g., "{{data-generator.output.user_age}}" -> "data-generator.output.user_age")
            Set<String> aliasRefs = templateService.extractRefs(aliasValue);
            if (!aliasRefs.isEmpty()) {
                String resolvedTemplate = aliasRefs.iterator().next();
                OutputUsage resolvedConsumer = nodeConsumers.get(resolvedTemplate);
                if (resolvedConsumer != null) {
                    log.debug("Resolved alias {} -> {} -> consumer found", template, resolvedTemplate);
                    return resolvedConsumer;
                }
            }
        }

        log.debug("No consumer found for template: {}", template);
        return null;
    }

    /**
     * Helper class to store field location information
     */
    private record FieldLocation(String path, String topLevelField) {
    }
}
