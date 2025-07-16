package org.phong.zenflow.workflow.subdomain.node_logs.dto;

import java.time.OffsetDateTime;

public record LogEntry(String timestamp, String level, String message) {
    public static LogEntry of(String level, String message) {
        return new LogEntry(getCurrentTimestamp(), level, message);
    }

    public static LogEntry success(String message) {
        return new LogEntry(getCurrentTimestamp(), "SUCCESS", message);
    }

    public static LogEntry info(String message) {
        return new LogEntry(getCurrentTimestamp(), "INFO", message);
    }

    public static LogEntry warning(String message) {
        return new LogEntry(getCurrentTimestamp(), "WARNING", message);
    }

    public static LogEntry error(String message) {
        return new LogEntry(getCurrentTimestamp(), "ERROR", message);
    }

    private static String getCurrentTimestamp() {
        return OffsetDateTime.now().toString();
    }
}
