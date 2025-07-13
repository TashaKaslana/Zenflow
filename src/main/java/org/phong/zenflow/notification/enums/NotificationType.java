package org.phong.zenflow.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum NotificationType {
    INFO("info"),
    WARNING("warning"),
    ERROR("error"),
    SUCCESS("success");

    private final String type;
}
