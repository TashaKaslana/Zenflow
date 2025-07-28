package org.phong.zenflow.workflow.subdomain.node_definition.exception;

public class WorkflowNodeDefinitionException extends RuntimeException {
    public WorkflowNodeDefinitionException(String message) {
        super(message);
    }

    public WorkflowNodeDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
