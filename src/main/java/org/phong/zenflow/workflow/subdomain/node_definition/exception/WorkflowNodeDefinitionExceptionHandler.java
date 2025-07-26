package org.phong.zenflow.workflow.subdomain.node_definition.exception;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.responses.ApiErrorResponse;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.core.utils.HttpRequestUtils;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@AllArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WorkflowNodeDefinitionExceptionHandler {
    @ExceptionHandler(WorkflowDefinitionValidationException.class)
    public ResponseEntity<RestApiResponse<Void>> handleWorkflowDefinitionValidationException(WorkflowDefinitionValidationException ex, WebRequest request) {
        log.debug("Workflow Definition Validation Exception occurred: {}", ex.getMessage());

        ValidationResult validationResult = ex.getValidationResult();
        Map<String, Object> fieldErrors = null;

        if (validationResult != null && validationResult.getErrors() != null) {
            try {
                fieldErrors = validationResult.getErrors().stream()
                        .filter(error -> error != null && error.getPath() != null)
                        .collect(Collectors.toMap(
                                ValidationError::getPath,
                                ObjectConversion::convertObjectToMap,
                                (existing, replacement) -> existing // In case of duplicate keys
                        ));
            } catch (Exception e) {
                log.warn("Error processing validation errors: {}", e.getMessage(), e);
                // Create a simplified error representation
                fieldErrors = Map.of("error", "Error processing validation details");
            }
        }

        if (fieldErrors == null) {
            fieldErrors = Map.of("error", "No detailed validation information available");
        }

        String requestPath = null;
        try {
            requestPath = HttpRequestUtils.getRequestPath(request);
        } catch (Exception e) {
            log.warn("Could not determine request path: {}", e.getMessage());
        }

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Workflow Definition Validation Error",
                requestPath,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                fieldErrors,
                null
        );

        return RestApiResponse.badRequest(errorResponse, "An error occurred with the Workflow Definition Validation: " + ex.getMessage());
    }

    @ExceptionHandler(WorkflowNodeDefinitionException.class)
    public ResponseEntity<RestApiResponse<Void>> handleWorkflowNodeDefinitionException(WorkflowNodeDefinitionException ex) {
        log.debug("WorkflowNodeDefinition exception occurred: {}", ex.getMessage());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "WorkflowNodeDefinition Error",
                ex.getMessage()
        );

        return RestApiResponse.badRequest(errorResponse, "An error occurred with the WorkflowNodeDefinition: " + ex.getMessage());
    }
}
