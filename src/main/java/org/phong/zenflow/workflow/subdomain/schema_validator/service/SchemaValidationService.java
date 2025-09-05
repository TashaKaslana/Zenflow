package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
import org.phong.zenflow.workflow.subdomain.execution.services.TemplateService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for validating data against JSON schemas and templates.
 * Provides methods to validate workflow components against their respective schemas
 * and ensure template expressions are correctly formed.
 */
@Service
@Slf4j
@AllArgsConstructor
public class SchemaValidationService {
    private final SchemaRegistry schemaRegistry;
    private final ObjectMapper objectMapper;
    private final SchemaTypeResolver schemaTypeResolver;
    private final TemplateService templateService;

    /**
     * Validates the supplied data against the JSON-Schema identified by {@code templateString}.
     *
     * @param nodeKey        logical key of the node being validated
     * @param data           Java object to validate (will be serialized with Jackson)
     * @param templateString identifies the canonical schema (e.g., builtin: …, UUID, …)
     * @param basePath       prefix that is stored in every ValidationError.path
     * @param slice          what part of the schema / data to validate.<br>
     *                       • "" or null → whole document<br>
     *                       • "input" → schema.properties.input against data.input<br>
     *                       • JSON-Pointer (starts with '/') → that pointer is applied to both
     * @param skipTemplateFields if true, skip validation for fields containing template strings
     *
     * @return list of ValidationErrors (empty if the instance is valid)
     */
    public List<ValidationError> validateAgainstSchema(
            String nodeKey,
            Object data,
            String templateString,
            String basePath,
            @Nullable String slice,
            boolean skipTemplateFields) {

        List<ValidationError> errors = new ArrayList<>();

        try {
            /* 1. Load the canonical schema */
            JSONObject fullSchemaJson = schemaRegistry.getSchemaByTemplateString(templateString);
            if (fullSchemaJson == null) {
                errors.add(ValidationError.builder()
                        .nodeKey(nodeKey)
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.SCHEMA_NOT_FOUND)
                        .path(basePath)
                        .message("Schema not found: " + templateString)
                        .build());
                return errors;
            }

            /* 2. Pick the slice of schema to use */
            JSONObject schemaJsonToUse = getFullJsonObject(slice, fullSchemaJson);

            /* 3. Serialize data and pick the matching slice from the instance */
            String dataJson = objectMapper.writeValueAsString(data);
            Object instanceToValidate = getInstanceToValidate(slice, dataJson);

            /* 4. Handle template field processing and value-based validation */
            if (instanceToValidate instanceof JSONObject originalInstance) {
                if (skipTemplateFields) {
                    // Definition phase: Replace template fields with type-appropriate placeholders
                    // This prevents "required key not found" errors while still validating structure

                    // First, validate wrong keys before replacing template fields
                    if (schemaJsonToUse.has("properties")) {
                        errors.addAll(validateWrongKeys(nodeKey, originalInstance, schemaJsonToUse.getJSONObject("properties"), basePath));
                    }

                    instanceToValidate = replaceTemplateFieldsWithPlaceholders(originalInstance,
                            schemaJsonToUse.has("properties") ? schemaJsonToUse.getJSONObject("properties") : null);
                    log.debug("Replaced template fields with placeholders for definition-phase validation");
                } else {
                    // Runtime phase: Perform full value-based validation including wrong keys
                    if (schemaJsonToUse.has("properties")) {
                        errors.addAll(validateValueTypes(nodeKey, originalInstance, schemaJsonToUse.getJSONObject("properties"), basePath));
                    }
                }
            }

            /* 5. Build the schema and validate */
            Schema schema = SchemaLoader.builder()
                    .schemaJson(schemaJsonToUse)
                    .draftV7Support()
                    .resolutionScope("classpath:/schemas/")
                    .build()
                    .load().build();

            schema.validate(instanceToValidate);

        } catch (ValidationException ve) {
            errors.addAll(convertValidationException(nodeKey, ve, basePath));
        } catch (Exception e) {
            errors.add(ValidationError.builder()
                    .nodeKey(nodeKey)
                    .errorType("definition")
                    .errorCode(ValidationErrorCode.VALIDATION_ERROR)
                    .path(basePath)
                    .message("Validation error: " + e.getMessage())
                    .build());
        }

        return errors;
    }

    /**
     * Backward compatibility method - defaults to not skipping template fields
     */
    public List<ValidationError> validateAgainstSchema(
            String nodeKey,
            Object data,
            String templateString,
            String basePath,
            @Nullable String slice) {
        return validateAgainstSchema(nodeKey, data, templateString, basePath, slice, false);
    }

    private static JSONObject getFullJsonObject(String slice, JSONObject fullSchemaJson) {
        JSONObject schemaJsonToUse;
        if (slice != null && !slice.isEmpty()) {
            if (slice.charAt(0) == '/') {
                JSONPointer ptr = new JSONPointer(slice);
                schemaJsonToUse = (JSONObject) ptr.queryFrom(fullSchemaJson);
            } else {
                // When slicing to a property, we need to preserve schema metadata if they exist
                JSONObject propertySchema = fullSchemaJson.getJSONObject("properties").getJSONObject(slice);

                // Check if the property schema contains references that might need the root schema context
                if (schemaContainsReferences(propertySchema)) {
                    // Create a new schema object that includes the property schema plus necessary root-level metadata
                    schemaJsonToUse = new JSONObject();
                    // Copy all properties from the property schema
                    propertySchema.keys().forEachRemaining(key ->
                        schemaJsonToUse.put(key, propertySchema.get(key))
                    );

                    // Preserve important root-level schema metadata that might be needed for reference resolution
                    preserveSchemaMetadata(fullSchemaJson, schemaJsonToUse);
                } else {
                    schemaJsonToUse = propertySchema;
                }
            }
        } else {
            schemaJsonToUse = fullSchemaJson;
        }
        return schemaJsonToUse;
    }

    /**
     * Preserves important schema metadata needed for reference resolution
     */
    private static void preserveSchemaMetadata(JSONObject fullSchema, JSONObject targetSchema) {
        // Preserve definitions if they exist (for internal references like #/definitions/...)
        if (fullSchema.has("definitions")) {
            targetSchema.put("definitions", fullSchema.getJSONObject("definitions"));
        }

        // Preserve $schema if it exists (maybe needed for external references)
        if (fullSchema.has("$schema")) {
            targetSchema.put("$schema", fullSchema.getString("$schema"));
        }

        // Preserve $id if it exists (base URI for resolving relative references)
        if (fullSchema.has("$id")) {
            targetSchema.put("$id", fullSchema.getString("$id"));
        }
    }

    /**
     * Checks if a JSON schema contains $ref references that might need definitions
     */
    private static boolean schemaContainsReferences(JSONObject schema) {
        return schema.toString().contains("$ref");
    }

    private static Object getInstanceToValidate(String slice, String dataJson) {
        JSONObject instance = new JSONObject(dataJson);
        Object instanceToValidate = instance;

        if (slice != null && !slice.isEmpty()) {
            if (slice.charAt(0) == '/') {
                JSONPointer pointer = new JSONPointer(slice);
                instanceToValidate = pointer.queryFrom(instance);
            } else {
                instanceToValidate = instance.opt(slice);
            }
        }
        return instanceToValidate;
    }

    /**
     * Validates template expressions found within the provided data object.
     * Ensures that all template references follow the correct syntax.
     *
     * @param data Object containing potential template expressions
     * @param basePath Base path used for error reporting
     * @return List of validation errors for invalid template expressions
     */
    public List<ValidationError> validateTemplates(String nodeKey, Object data, String basePath) {
        List<ValidationError> errors = new ArrayList<>();

        Set<String> templates = templateService.extractRefs(data);
        for (String template : templates) {
            String fullTemplate = "{{" + template + "}}";
            if (!templateService.isTemplate(fullTemplate)) {
                errors.add(ValidationError.builder()
                        .nodeKey(nodeKey)
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.INVALID_TEMPLATE_EXPRESSION)
                        .path(basePath)
                        .message("Invalid template expression: " + fullTemplate)
                        .template(fullTemplate)
                        .build());
            }
        }

        return errors;
    }

    /**
     * Converts ValidationException objects into ValidationError objects.
     * Handles both the main violation and any nested violations recursively.
     *
     * @param e ValidationException to convert
     * @param basePath Base path to use for error reporting
     * @return List of ValidationError objects representing all validation errors
     */
    private List<ValidationError> convertValidationException(String nodeKey, ValidationException e, String basePath) {
        List<ValidationError> errors = new ArrayList<>();

        // Handle the main violation
        errors.add(ValidationError.builder()
                .nodeKey(nodeKey)
                .errorType("definition")
                .errorCode(ValidationErrorCode.INVALID_SCHEMA)
                .path(buildPath(basePath, e.getPointerToViolation()))
                .message(e.getMessage())
                .schemaPath(e.getSchemaLocation())
                .build());

        // Handle nested violations
        for (ValidationException cause : e.getCausingExceptions()) {
            errors.addAll(convertValidationException(nodeKey, cause, basePath));
        }

        return errors;
    }

    /**
     * Builds a properly formatted path by combining the base path with a JSON pointer.
     * Converts JSON pointer notation to dot notation for consistent error reporting.
     *
     * @param basePath Base path to prepend
     * @param pointer JSON pointer to the validation violation
     * @return Formatted path string in dot notation
     */
    private String buildPath(String basePath, String pointer) {
        if (pointer == null || pointer.isEmpty() || pointer.equals("#")) {
            return basePath;
        }

        String cleanPointer = pointer.startsWith("#/") ? pointer.substring(2) : pointer;
        cleanPointer = cleanPointer.replace("/", ".");

        if (basePath.isEmpty()) {
            return cleanPointer;
        }

        return basePath + "." + cleanPointer;
    }

    /**
     * Replaces template fields with type-appropriate placeholder values instead of removing them.
     * This prevents "required key not found" errors during definition-phase validation.
     *
     * @param jsonObject The JSON object to process
     * @param schemaProperties The schema properties to determine appropriate placeholder types
     * @return A new JSON object with template fields replaced by type-appropriate placeholders
     */
    private JSONObject replaceTemplateFieldsWithPlaceholders(JSONObject jsonObject, JSONObject schemaProperties) {
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
     * Gets an appropriate placeholder value for a schema field based on its expected type
     */
    private Object getPlaceholderForSchemaField(String fieldName, JSONObject schemaProperties) {
        if (schemaProperties == null || !schemaProperties.has(fieldName)) {
            return "__TEMPLATE_PLACEHOLDER__"; // Default string placeholder
        }

        JSONObject fieldSchema = schemaProperties.getJSONObject(fieldName);
        String expectedType = schemaTypeResolver.getSchemaType(fieldSchema);

        return switch (expectedType) {
            case "integer" -> 0;
            case "number" -> 0.0;
            case "boolean" -> false;
            case "array" -> new JSONArray();
            case "object" -> new JSONObject();
            default -> "__TEMPLATE_PLACEHOLDER__";
        };
    }

    /**
     * Validates value types of the instance against the expected schema properties.
     * Handles wrong keys by attempting to match values with expected field types.
     * Now uses SchemaTypeResolver for consistent type handling.
     *
     * @param nodeKey Logical key of the node being validated
     * @param instance The instance data to validate
     * @param schemaProperties The expected schema properties for validation
     * @param basePath Base path used for error reporting
     * @return List of validation errors for mismatched value types and wrong keys
     */
    private List<ValidationError> validateValueTypes(String nodeKey, JSONObject instance, JSONObject schemaProperties, String basePath) {
        List<ValidationError> errors = new ArrayList<>();

        // First pass: Validate existing keys in both instance and schema
        for (String key : schemaProperties.keySet()) {
            if (instance.has(key)) {
                // Normal validation for correct keys using SchemaTypeResolver
                JSONObject fieldSchema = schemaProperties.getJSONObject(key);
                Object value = instance.get(key);
                errors.addAll(validateFieldTypeWithResolver(nodeKey, key, value, fieldSchema, basePath));
            }
        }

        // Second pass: Handle wrong keys using the consolidated method
        errors.addAll(validateWrongKeys(nodeKey, instance, schemaProperties, basePath));

        return errors;
    }

    /**
     * Validates wrong keys in the instance data.
     * This method is used by both definition and runtime phase validation.
     *
     * @param nodeKey Logical key of the node being validated
     * @param instance The instance data to validate
     * @param schemaProperties The expected schema properties for validation
     * @param basePath Base path used for error reporting
     * @return List of validation errors for wrong keys
     */
    private List<ValidationError> validateWrongKeys(String nodeKey, JSONObject instance, JSONObject schemaProperties, String basePath) {
        List<ValidationError> errors = new ArrayList<>();

        // Check for wrong keys in instance (keys not in schema)
        for (String instanceKey : instance.keySet()) {
            if (!schemaProperties.has(instanceKey)) {
                // This key doesn't exist in schema - it's a wrong key
                Object instanceValue = instance.get(instanceKey);

                // Skip template strings - they'll be validated at runtime
                if (instanceValue instanceof String && templateService.isTemplate((String) instanceValue)) {
                    continue;
                }

                // Try to find a compatible field type in schema using SchemaTypeResolver
                String suggestedKey = findCompatibleSchemaFieldWithResolver(instanceValue, schemaProperties);

                ValidationError.ValidationErrorBuilder errorBuilder = ValidationError.builder()
                        .nodeKey(nodeKey)
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.INVALID_SCHEMA)
                        .path(buildPath(basePath, instanceKey));

                if (suggestedKey != null) {
                    errorBuilder.message(String.format(
                            "Unknown key '%s'. Did you mean '%s'? The value type '%s' matches the expected type for '%s'.",
                            instanceKey, suggestedKey, schemaTypeResolver.getValueTypeName(instanceValue), suggestedKey))
                            .expectedType("valid_schema_key")
                            .value(instanceKey);
                } else {
                    errorBuilder.message(String.format(
                            "Unknown key '%s'. This key is not defined in the schema. Value type: '%s'",
                            instanceKey, schemaTypeResolver.getValueTypeName(instanceValue)))
                            .expectedType("valid_schema_key")
                            .value(instanceKey);
                }

                errors.add(errorBuilder.build());
            }
        }

        return errors;
    }

    /**
     * Validates a single field's type against its schema definition using SchemaTypeResolver
     */
    private List<ValidationError> validateFieldTypeWithResolver(String nodeKey, String key, Object value, JSONObject fieldSchema, String basePath) {
        List<ValidationError> errors = new ArrayList<>();

        String expectedType = schemaTypeResolver.getSchemaType(fieldSchema);
        String actualType = schemaTypeResolver.getJsonValueType(value);

        if (!schemaTypeResolver.isTypeCompatible(expectedType, actualType)) {
            errors.add(ValidationError.builder()
                    .nodeKey(nodeKey)
                    .errorType("definition")
                    .errorCode(ValidationErrorCode.INVALID_TYPE)
                    .path(buildPath(basePath, key))
                    .message(String.format(
                            "Expected type '%s' for key '%s', but got '%s'",
                            expectedType, key, schemaTypeResolver.getValueTypeName(value)))
                    .expectedType(expectedType)
                    .value(schemaTypeResolver.getValueTypeName(value))
                    .build());
        }

        return errors;
    }

    /**
     * Tries to find a schema field that's compatible with the given value type using SchemaTypeResolver
     */
    private String findCompatibleSchemaFieldWithResolver(Object value, JSONObject schemaProperties) {
        String valueType = schemaTypeResolver.getJsonValueType(value);

        for (String schemaKey : schemaProperties.keySet()) {
            Object schemaField = schemaProperties.get(schemaKey);
            if (schemaField instanceof JSONObject fieldSchema) {
                String expectedType = schemaTypeResolver.getSchemaType(fieldSchema);
                if (schemaTypeResolver.isTypeCompatible(expectedType, valueType)) {
                    return schemaKey; // Found a compatible field
                }
            }
        }

        return null; // No compatible field found
    }
}
