package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
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
     *
     * @return list of ValidationErrors (empty if the instance is valid)
     */
    public List<ValidationError> validateAgainstSchema(
            String nodeKey,
            Object data,
            String templateString,
            String basePath,
            @Nullable String slice) {

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

            /* 3. Serialise data and pick the matching slice from the instance */
            String dataJson = objectMapper.writeValueAsString(data);
            Object instanceToValidate = getInstanceToValidate(slice, dataJson);

            /* 4. Build the schema and validate */
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

    private static JSONObject getFullJsonObject(String slice, JSONObject fullSchemaJson) {
        JSONObject schemaJsonToUse = fullSchemaJson;          // default = whole schema
        if (slice != null && !slice.isEmpty()) {
            if (slice.charAt(0) == '/') {
                JSONPointer ptr = new JSONPointer(slice);
                schemaJsonToUse = (JSONObject) ptr.queryFrom(fullSchemaJson);
            } else {
                schemaJsonToUse = fullSchemaJson.getJSONObject("properties").getJSONObject(slice);
            }
        }
        return schemaJsonToUse;
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

        Set<String> templates = TemplateEngine.extractRefs(data);
        for (String template : templates) {
            String fullTemplate = "{{" + template + "}}";
            if (!TemplateEngine.isTemplate(fullTemplate)) {
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

        // Handle main violation
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
}
