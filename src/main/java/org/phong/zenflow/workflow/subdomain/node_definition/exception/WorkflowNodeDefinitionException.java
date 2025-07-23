package org.phong.zenflow.workflow.subdomain.node_definition.exception;

import org.phong.zenflow.workflow.exception.WorkflowException;

public class WorkflowNodeDefinitionException extends WorkflowException {
    public WorkflowNodeDefinitionException(String message) {
        super(message);
    }

    public WorkflowNodeDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
