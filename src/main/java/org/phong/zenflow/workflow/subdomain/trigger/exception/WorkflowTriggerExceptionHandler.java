package org.phong.zenflow.workflow.subdomain.trigger.exception;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.responses.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WorkflowTriggerExceptionHandler {

    @ExceptionHandler(WorkflowTriggerException.WorkflowTriggerNotFound.class)
    public ResponseEntity<ErrorResponse> handleWorkflowTriggerNotFound(WorkflowTriggerException.WorkflowTriggerNotFound ex) {
        log.error("Workflow trigger not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .errorCode("WORKFLOW_TRIGGER_NOT_FOUND")
                        .build());
    }

    @ExceptionHandler(WorkflowTriggerException.WorkflowTriggerAlreadyExists.class)
    public ResponseEntity<ErrorResponse> handleWorkflowTriggerAlreadyExists(WorkflowTriggerException.WorkflowTriggerAlreadyExists ex) {
        log.error("Workflow trigger already exists", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .errorCode("WORKFLOW_TRIGGER_ALREADY_EXISTS")
                        .build());
    }

    @ExceptionHandler(WorkflowTriggerException.InvalidTriggerConfiguration.class)
    public ResponseEntity<ErrorResponse> handleInvalidTriggerConfiguration(WorkflowTriggerException.InvalidTriggerConfiguration ex) {
        log.error("Invalid trigger configuration", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .errorCode("INVALID_TRIGGER_CONFIGURATION")
                        .build());
    }

    @ExceptionHandler(WorkflowTriggerException.TriggerExecutionFailure.class)
    public ResponseEntity<ErrorResponse> handleTriggerExecutionFailure(WorkflowTriggerException.TriggerExecutionFailure ex) {
        log.error("Trigger execution failure", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .errorCode("TRIGGER_EXECUTION_FAILURE")
                        .build());
    }

    @ExceptionHandler(WorkflowTriggerException.class)
    public ResponseEntity<ErrorResponse> handleGenericWorkflowTriggerException(WorkflowTriggerException ex) {
        log.error("Workflow trigger error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .errorCode("WORKFLOW_TRIGGER_ERROR")
                        .build());
    }
}
