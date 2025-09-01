package org.phong.zenflow.workflow.subdomain.node_execution.enums;

public enum NodeExecutionStatus {
    RUNNING,
    SUCCESS,
    ERROR,
    WAITING,
    RETRYING,
    NEXT,
    LOOP_NEXT,
    LOOP_END;

    public boolean isFailure() {
        return this == ERROR || this == RETRYING;
    }
}
