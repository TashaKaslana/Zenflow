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
}
