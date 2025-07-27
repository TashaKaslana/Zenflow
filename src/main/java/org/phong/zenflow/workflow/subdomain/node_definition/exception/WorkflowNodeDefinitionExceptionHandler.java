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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@AllArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WorkflowNodeDefinitionExceptionHandler {
    @ExceptionHandler(WorkflowDefinitionValidationException.class)
    public ResponseEntity<RestApiResponse<Void>> handleWorkflowDefinitionValidationException(WorkflowDefinitionValidationException ex, WebRequest request) {
        log.debug("Workflow Definition Validation Exception occurred: {}", ex.getMessage());

        ValidationResult validationResult = ex.getValidationResult();
        Map<String, Object> groupedErrors = new HashMap<>();

        for (ValidationError error : validationResult.getErrors()) {
            if (error == null) continue;

            String nodeKey = error.getNodeKey() != null ? error.getNodeKey() : "workflow";
            Map<String, Object> errorMap = ObjectConversion.convertObjectToMap(error);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> errorList = (List<Map<String, Object>>) groupedErrors
                    .computeIfAbsent(nodeKey, k -> new ArrayList<Map<String, Object>>());

            errorList.add(errorMap);
        }

        String requestPath = null;
        try {
            requestPath = HttpRequestUtils.getRequestPath(request);
        } catch (Exception e) {
            log.warn("Could not determine request path: {}", e.getMessage());
        }

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Workflow Definition Validation Error in Phase: " + validationResult.getPhase(),
                requestPath,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                groupedErrors,
                null
        );

        return RestApiResponse.badRequest(errorResponse, "Workflow definition validation failed: " + ex.getMessage());
    }

    @ExceptionHandler(WorkflowNodeDefinitionException.class)
    public ResponseEntity<RestApiResponse<Void>> handleWorkflowNodeDefinitionException(WorkflowNodeDefinitionException ex, WebRequest request) {
        log.debug("WorkflowNodeDefinition exception occurred: {}", ex.getMessage());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpRequestUtils.getRequestPath(request),
                "WorkflowNodeDefinition Error",
                ex.getMessage()
        );

        return RestApiResponse.badRequest(errorResponse, "An error occurred with the WorkflowNodeDefinition: " + ex.getMessage());
    }
}
