package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.plugin.subdomain.node.utils.SchemaRegistry;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
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
     * Validates data against a specified JSON schema identified by a template string.
     *
     * @param data Object to validate against the schema
     * @param templateString String identifier for retrieving the schema
     * @param basePath Base path used for error reporting
     * @return List of validation errors, empty if validation is successful
     */
    public List<ValidationError> validateAgainstSchema(String nodeKey, Object data, String templateString, String basePath) {
        List<ValidationError> errors = new ArrayList<>();

        try {
            JSONObject schemaJson = schemaRegistry.getSchemaByTemplateString(templateString);
            if (schemaJson == null) {
                errors.add(ValidationError.builder()
                        .nodeKey(nodeKey)
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.SCHEMA_NOT_FOUND)
                        .path(basePath)
                        .message("Schema not found: " + templateString)
                        .build());
                return errors;
            }

            // Convert data to JSON
            String dataJson = objectMapper.writeValueAsString(data);
            JSONObject jsonObject = new JSONObject(new JSONTokener(dataJson));

            Schema schema = SchemaLoader.load(schemaJson);

            // Perform validation
            schema.validate(jsonObject);

            // Additional template-specific validation
            errors.addAll(validateTemplates(nodeKey, data, basePath));

        } catch (ValidationException e) {
            errors.addAll(convertValidationException(nodeKey, e, basePath));
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
