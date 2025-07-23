package org.phong.zenflow.workflow.subdomain.engine.dto;

public enum WorkflowExecutionStatus {
    /**
     * The workflow ran through all nodes to the end.
     */
    COMPLETED,

    /**
     * The workflow stopped mid-way due to a RETRY or WAITING state.
     */
    HALTED
}

