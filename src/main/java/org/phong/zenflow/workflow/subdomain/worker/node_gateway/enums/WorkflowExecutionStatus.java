package org.phong.zenflow.workflow.subdomain.worker.node_gateway.enums;

import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;

public enum WorkflowExecutionStatus {
    COMPLETED,

    SUCCESS,
    FAILED,
    ERROR,
    PENDING,
    PROCESSING;

    public static WorkflowExecutionStatus mapStatus(ExecutionStatus status) {
        return switch (status) {
            case SUCCESS,
                 NEXT,
                 LOOP_NEXT,
                 LOOP_END,
                 LOOP_CONTINUE,
                 LOOP_BREAK,
                 COMMIT,
                 UNCOMMIT -> WorkflowExecutionStatus.SUCCESS;
            case RETRY,
                 VALIDATION_ERROR -> WorkflowExecutionStatus.FAILED;
            case ERROR -> WorkflowExecutionStatus.ERROR;
            case WAITING -> PROCESSING;
        };
    }
}
