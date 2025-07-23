package org.phong.zenflow.workflow.subdomain.node_logs.utils;

import org.phong.zenflow.workflow.subdomain.node_logs.dto.LogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogCollector {

    private final List<LogEntry> logs = new ArrayList<>();

    public void info(String message, Object... args) {
        logs.add(LogEntry.info(formatWithBraces(message, args)));
    }

    public void success(String message, Object... args) {
        logs.add(LogEntry.success(formatWithBraces(message, args)));
    }

    public void warning(String message, Object... args) {
        logs.add(LogEntry.warning(formatWithBraces(message, args)));
    }

    public void error(String message, Object... args) {
        logs.add(LogEntry.error(formatWithBraces(message, args)));
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

    private String formatWithBraces(String message, Object... args) {
        if (message == null || message.isEmpty()) return "";
        if (args == null) args = new Object[0];

        StringBuilder result = new StringBuilder();
        int length = message.length();
        int argIndex = 0;

        for (int i = 0; i < length; i++) {
            char ch = message.charAt(i);

            if (ch == '\\') {
                // Handle escaped brace: \{}
                if (i + 2 < length && message.charAt(i + 1) == '{' && message.charAt(i + 2) == '}') {
                    result.append("{}");
                    i += 2;
                } else {
                    // Just a backslash or unrelated escape
                    result.append(ch);
                    if (i + 1 < length) {
                        result.append(message.charAt(i + 1));
                        i++;
                    }
                }
            } else if (ch == '{' && i + 1 < length && message.charAt(i + 1) == '}') {
                // Replace {} with argument
                if (argIndex < args.length) {
                    Object arg = args[argIndex++];
                    result.append(arg == null ? "null" : arg.toString());
                } else {
                    result.append("{}"); // Not enough args — keep literal
                }
                i++; // Skip '}'
            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }
}
