package org.phong.zenflow.workflow.subdomain.engine.exception;

import org.phong.zenflow.workflow.exception.WorkflowException;

public class WorkflowEngineException extends WorkflowException {
    public WorkflowEngineException(String message) {
        super(message);
    }

    public WorkflowEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
