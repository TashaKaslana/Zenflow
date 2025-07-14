package org.phong.zenflow.workflow.subdomain.workflow_run.exception;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.responses.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WorkflowRunExceptionHandler {

    @ExceptionHandler(WorkflowRunException.WorkflowRunNotFound.class)
    public ResponseEntity<ErrorResponse> handleWorkflowRunNotFound(WorkflowRunException.WorkflowRunNotFound ex) {
        log.error("Workflow run not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .errorCode("WORKFLOW_RUN_NOT_FOUND")
                        .build());
    }

    @ExceptionHandler(WorkflowRunException.WorkflowRunAlreadyCompleted.class)
    public ResponseEntity<ErrorResponse> handleWorkflowRunAlreadyCompleted(WorkflowRunException.WorkflowRunAlreadyCompleted ex) {
        log.error("Workflow run already completed", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .errorCode("WORKFLOW_RUN_ALREADY_COMPLETED")
                        .build());
    }

    @ExceptionHandler(WorkflowRunException.InvalidWorkflowRunState.class)
    public ResponseEntity<ErrorResponse> handleInvalidWorkflowRunState(WorkflowRunException.InvalidWorkflowRunState ex) {
        log.error("Invalid workflow run state", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .errorCode("INVALID_WORKFLOW_RUN_STATE")
                        .build());
    }

    @ExceptionHandler(WorkflowRunException.WorkflowRunExecutionFailure.class)
    public ResponseEntity<ErrorResponse> handleWorkflowRunExecutionFailure(WorkflowRunException.WorkflowRunExecutionFailure ex) {
        log.error("Workflow run execution failure", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .errorCode("WORKFLOW_RUN_EXECUTION_FAILURE")
                        .build());
    }

    @ExceptionHandler(WorkflowRunException.WorkflowRunTimeout.class)
    public ResponseEntity<ErrorResponse> handleWorkflowRunTimeout(WorkflowRunException.WorkflowRunTimeout ex) {
        log.error("Workflow run timeout", ex);
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .errorCode("WORKFLOW_RUN_TIMEOUT")
                        .build());
    }

    @ExceptionHandler(WorkflowRunException.class)
    public ResponseEntity<ErrorResponse> handleGenericWorkflowRunException(WorkflowRunException ex) {
        log.error("Workflow run error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .errorCode("WORKFLOW_RUN_ERROR")
                        .build());
    }
}
