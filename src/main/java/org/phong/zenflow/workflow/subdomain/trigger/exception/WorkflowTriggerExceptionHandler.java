package org.phong.zenflow.workflow.subdomain.trigger.exception;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class WorkflowTriggerExceptionHandler {

    @ExceptionHandler(WorkflowTriggerException.WorkflowTriggerNotFound.class)
    public ResponseEntity<RestApiResponse<Void>> handleWorkflowTriggerNotFound(
            WorkflowTriggerException.WorkflowTriggerNotFound ex, WebRequest request) {
        log.error("Workflow trigger not found", ex);
        return RestApiResponse.notFound(request.getDescription(false), ex.getMessage());
    }

    @ExceptionHandler(WorkflowTriggerException.WorkflowTriggerAlreadyExists.class)
    public ResponseEntity<RestApiResponse<Void>> handleWorkflowTriggerAlreadyExists(
            WorkflowTriggerException.WorkflowTriggerAlreadyExists ex, WebRequest request) {
        log.error("Workflow trigger already exists", ex);
        return RestApiResponse.conflict(request.getDescription(false), ex.getMessage());
    }

    @ExceptionHandler(WorkflowTriggerException.InvalidTriggerConfiguration.class)
    public ResponseEntity<RestApiResponse<Void>> handleInvalidTriggerConfiguration(
            WorkflowTriggerException.InvalidTriggerConfiguration ex, WebRequest request) {
        log.error("Invalid trigger configuration", ex);
        return RestApiResponse.badRequest(request.getDescription(false), ex.getMessage());
    }

    @ExceptionHandler(WorkflowTriggerException.TriggerExecutionFailure.class)
    public ResponseEntity<RestApiResponse<Void>> handleTriggerExecutionFailure(
            WorkflowTriggerException.TriggerExecutionFailure ex, WebRequest request) {
        log.error("Trigger execution failure", ex);
        return RestApiResponse.internalServerError(request.getDescription(false), ex.getMessage());
    }

    @ExceptionHandler(WorkflowTriggerException.class)
    public ResponseEntity<RestApiResponse<Void>> handleGenericWorkflowTriggerException(
            WorkflowTriggerException ex, WebRequest request) {
        log.error("Workflow trigger error", ex);
        return RestApiResponse.internalServerError(request.getDescription(false), ex.getMessage());
    }
}
