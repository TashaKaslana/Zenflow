package org.phong.zenflow.workflow.subdomain.workflow_run.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TriggerType {
    MANUAL("MANUAL"),
    SCHEDULE("SCHEDULE"),
    WEBHOOK("WEBHOOK"),
    EVENT("EVENT");

    private final String type;
}
