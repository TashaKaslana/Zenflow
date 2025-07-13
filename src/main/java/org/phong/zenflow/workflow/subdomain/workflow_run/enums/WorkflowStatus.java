package org.phong.zenflow.workflow.subdomain.workflow_run.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum WorkflowStatus {
    RUNNING("RUNNING"),
    SUCCESS("SUCCESS"),
    ERROR("ERROR"),
    WAITING("WAITING");

    private final String status;
}
