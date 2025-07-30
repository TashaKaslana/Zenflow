package org.phong.zenflow.plugin.subdomain.execution.enums;

public enum ExecutionStatus {
    SUCCESS,
    ERROR,
    WAITING,
    RETRY,
    VALIDATION_ERROR,
    NEXT,
    LOOP_NEXT,
    LOOP_END,
    LOOP_CONTINUE,
    LOOP_BREAK;

    public static ExecutionStatus fromString(String status) {
        for (ExecutionStatus executionStatus : ExecutionStatus.values()) {
            if (executionStatus.name().equalsIgnoreCase(status)) {
                return executionStatus;
            }
        }
        throw new IllegalArgumentException("Unknown execution status: " + status);
    }
}
