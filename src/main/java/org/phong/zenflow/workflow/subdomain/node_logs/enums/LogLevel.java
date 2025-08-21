package org.phong.zenflow.workflow.subdomain.node_logs.enums;

public enum LogLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR, DEBUG;

    public String getName() {
        return name().toLowerCase();
    }

    public static LogLevel fromString(String level) {
        if (level == null || level.isEmpty()) {
            return INFO; // Default to INFO if null or empty
        }
        try {
            return LogLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INFO; // Default to INFO if the level is invalid
        }
    }
}
