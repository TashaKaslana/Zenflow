package org.phong.zenflow.workflow.subdomain.workflow_run.exception;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class WorkflowRunExceptionHandler {
    @ExceptionHandler(WorkflowRunException.class)
    public ResponseEntity<RestApiResponse<Void>> handleGenericWorkflowRunException(
            WorkflowRunException ex, WebRequest request) {
        log.error("Workflow run error", ex);
        return RestApiResponse.internalServerError(request.getDescription(false), ex.getMessage());
    }
}
