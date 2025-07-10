package org.phong.zenflow.log.systemlog.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SystemLogType {
    INFO("info"),
    WARNING("warning"),
    ERROR("error"),
    STARTUP("startup"),
    SCHEDULE("schedule"),
    PLUGIN("plugin"),
    OTHER("other");

    private final String value;

    SystemLogType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static SystemLogType fromValue(String value) {
        for (SystemLogType type : SystemLogType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown log type: " + value);
    }

    @JsonValue
    public String toValue() {
        return value;
    }
}
