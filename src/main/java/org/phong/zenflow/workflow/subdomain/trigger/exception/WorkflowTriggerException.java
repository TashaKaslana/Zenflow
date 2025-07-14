package org.phong.zenflow.workflow.subdomain.trigger.exception;

import org.phong.zenflow.core.exception.BaseException;

public class WorkflowTriggerException extends BaseException {
    public WorkflowTriggerException(String message) {
        super(message);
    }

    public WorkflowTriggerException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class WorkflowTriggerNotFound extends WorkflowTriggerException {
        public WorkflowTriggerNotFound(String triggerId) {
            super("Workflow trigger not found with ID: " + triggerId);
        }
    }

    public static class WorkflowTriggerAlreadyExists extends WorkflowTriggerException {
        public WorkflowTriggerAlreadyExists(String workflowId, String triggerType) {
            super("Workflow trigger already exists for workflow ID: " + workflowId + " with type: " + triggerType);
        }
    }

    public static class InvalidTriggerConfiguration extends WorkflowTriggerException {
        public InvalidTriggerConfiguration(String message) {
            super("Invalid trigger configuration: " + message);
        }
    }

    public static class TriggerExecutionFailure extends WorkflowTriggerException {
        public TriggerExecutionFailure(String message, Throwable cause) {
            super("Trigger execution failed: " + message, cause);
        }
    }
}
