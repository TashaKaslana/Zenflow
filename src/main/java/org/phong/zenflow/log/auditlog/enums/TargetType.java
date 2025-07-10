package org.phong.zenflow.log.auditlog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TargetType {
    USER("user"),
    WORKFLOW("workflow"),
    PROJECT("project"),
    SETTING("setting"),
    PLUGIN("plugin"),
    ROLE("role"),
    PERMISSION("permission"),
    OTHER("other");

    private final String value;
}
