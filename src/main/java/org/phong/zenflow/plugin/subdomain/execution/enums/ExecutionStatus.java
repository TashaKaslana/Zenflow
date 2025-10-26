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
    LOOP_BREAK,
    COMMIT,
    UNCOMMIT,
    ;

    public static ExecutionStatus fromString(String status) {
        for (ExecutionStatus executionStatus : ExecutionStatus.values()) {
            if (executionStatus.name().equalsIgnoreCase(status)) {
                return executionStatus;
            }
        }
        throw new IllegalArgumentException("Unknown execution status: " + status);
    }

    public static boolean isSuccessful(ExecutionStatus status) {
        return status == SUCCESS || status == NEXT || status == LOOP_NEXT || status == LOOP_END
            || status == LOOP_CONTINUE || status == LOOP_BREAK || status == COMMIT || status == UNCOMMIT;
    }
}
