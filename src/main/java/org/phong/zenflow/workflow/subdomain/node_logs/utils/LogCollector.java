package org.phong.zenflow.workflow.subdomain.node_logs.utils;


import org.phong.zenflow.workflow.subdomain.node_logs.dto.LogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogCollector {

    private final List<LogEntry> logs = new ArrayList<>();

    public void info(String message) {
        logs.add(LogEntry.info(message));
    }

    public void success(String message) {
        logs.add(LogEntry.success(message));
    }

    public void warning(String message) {
        logs.add(LogEntry.warning(message));
    }

    public void error(String message) {
        logs.add(LogEntry.error(message));
    }

    public List<LogEntry> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    public boolean isEmpty() {
        return logs.isEmpty();
    }

    public int size() {
        return logs.size();
    }

    public void clear() {
        logs.clear();
    }
}
