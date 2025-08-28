package org.phong.zenflow.workflow.subdomain.trigger.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TriggerType {
    MANUAL("MANUAL"),
    SCHEDULE("SCHEDULE"),
    WEBHOOK("WEBHOOK"),
    EVENT("EVENT"),
    POLLING("POLLING");

    private final String type;
}
