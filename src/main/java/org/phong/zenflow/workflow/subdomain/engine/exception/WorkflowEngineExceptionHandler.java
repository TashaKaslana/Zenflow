package org.phong.zenflow.workflow.subdomain.engine.exception;

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
public class WorkflowEngineExceptionHandler {
     @ExceptionHandler(WorkflowEngineException.class)
     public ResponseEntity<RestApiResponse<Void>> handleWorkflowEngineException(WorkflowEngineException ex) {
         log.debug("WorkflowEngine exception occurred: {}", ex.getMessage());

         ApiErrorResponse errorResponse = new ApiErrorResponse(
                 HttpStatus.BAD_REQUEST.value(),
                 "WorkflowEngine Error",
                 ex.getMessage()
         );

         return RestApiResponse.badRequest(errorResponse, "An error occurred with the WorkflowEngine: " + ex.getMessage());
     }

    @ExceptionHandler(WorkflowEngineValidationException.class)
    public ResponseEntity<RestApiResponse<Void>> handleWorkflowNodeDefinitionException(WorkflowEngineValidationException ex, WebRequest request) {
        log.debug("WorkflowEngine Validation Exception occurred: {}", ex.getMessage());

        ValidationResult validationResult = ex.getValidationResult();

        Map<String, Object> fieldErrors = validationResult.getErrors().stream()
                .collect(Collectors.toMap(
                        ValidationError::getPath,
                        ObjectConversion::convertObjectToFilteredMap
                ));

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "WorkflowEngine Validation Error",
                HttpRequestUtils.getRequestPath(request),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                fieldErrors,
                null
        );

        return RestApiResponse.badRequest(errorResponse, "An error occurred with the Workflow Engine Validation: " + ex.getMessage());
    }
}
