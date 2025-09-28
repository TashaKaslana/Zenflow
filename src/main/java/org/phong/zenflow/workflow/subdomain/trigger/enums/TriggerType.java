package org.phong.zenflow.workflow.subdomain.trigger.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TriggerType {
    MANUAL("MANUAL"),
    SCHEDULE("SCHEDULE"),
    SCHEDULE_RETRY("SCHEDULE_RETRY"),
    SCHEDULE_TIMEOUT("SCHEDULE_TIMEOUT"),
    WEBHOOK("WEBHOOK"),
    EVENT("EVENT"),
    POLLING("POLLING");

    private final String type;
}
