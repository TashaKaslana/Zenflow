package org.phong.zenflow.workflow.subdomain.node_logs.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.phong.zenflow.workflow.subdomain.node_logs.enums.LogLevel;
import org.phong.zenflow.workflow.subdomain.node_logs.logging.LogContextManager;
import org.phong.zenflow.workflow.subdomain.node_logs.logging.LogEntry;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * NodeLogPublisher is responsible for publishing log entries related to workflow nodes.
 * It provides methods to log messages at different levels (success, debug, info, warn, error)
 * and includes metadata such as workflow ID, run ID, node key, and user ID.
 */
@AllArgsConstructor
@Builder
@SuppressWarnings("unused")
public class NodeLogPublisher {
    private final ApplicationEventPublisher publisher;
    private final UUID workflowId;
    private final UUID runId;
    private final String nodeKey;
    private final UUID userId;

    // Basic logging methods
    public void success(String message) {
        LogEntry success = init(LogLevel.SUCCESS, message, null, null, null);
        publisher.publishEvent(success);
    }

    public void debug(String message) {
        LogEntry debug = init(LogLevel.DEBUG, message, null, null, null);
        publisher.publishEvent(debug);
    }

    public void info(String message) {
        LogEntry info = init(LogLevel.INFO, message, null, null, null);
        publisher.publishEvent(info);
    }

    public void warn(String message) {
        LogEntry warn = init(LogLevel.WARNING, message, null, null, null);
        publisher.publishEvent(warn);
    }

    // Parameterized logging methods
    public void success(String format, Object... args) {
        String formattedMessage = formatMessage(format, args);
        LogEntry success = init(LogLevel.SUCCESS, formattedMessage, null, null, null);
        publisher.publishEvent(success);
    }

    public void debug(String format, Object... args) {
        String formattedMessage = formatMessage(format, args);
        LogEntry debug = init(LogLevel.DEBUG, formattedMessage, null, null, null);
        publisher.publishEvent(debug);
    }

    public void info(String format, Object... args) {
        String formattedMessage = formatMessage(format, args);
        LogEntry info = init(LogLevel.INFO, formattedMessage, null, null, null);
        publisher.publishEvent(info);
    }

    public void warn(String format, Object... args) {
        String formattedMessage = formatMessage(format, args);
        LogEntry warn = init(LogLevel.WARNING, formattedMessage, null, null, null);
        publisher.publishEvent(warn);
    }

    // Builder methods for advanced cases
    public LogBuilder successBuilder(String message) {
        return new LogBuilder(LogLevel.SUCCESS, message);
    }

    public LogBuilder debugBuilder(String message) {
        return new LogBuilder(LogLevel.DEBUG, message);
    }

    public LogBuilder infoBuilder(String message) {
        return new LogBuilder(LogLevel.INFO, message);
    }

    public LogBuilder warnBuilder(String message) {
        return new LogBuilder(LogLevel.WARNING, message);
    }

    public ErrorLogBuilder error(String message) {
        return new ErrorLogBuilder(message);
    }

    public ErrorLogBuilder error(String format, Object... args) {
        return new ErrorLogBuilder(formatMessage(format, args));
    }

    /**
     * Error log builder with additional error context
     */
    public class ErrorLogBuilder {
        private final String message;
        private String errorCode;
        private Object meta;

        private ErrorLogBuilder(String message) {
            this.message = message;
        }

        public ErrorLogBuilder withErrorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public ErrorLogBuilder withMeta(Object meta) {
            this.meta = meta;
            return this;
        }

        public void log() {
            LogEntry error = init(LogLevel.ERROR, message, errorCode, null, meta);
            publisher.publishEvent(error);
        }
    }

    /**
     * Fluent builder for regular log entries
     */
    public class LogBuilder {
        private final LogLevel level;
        private final String message;
        private Object meta;

        private LogBuilder(LogLevel level, String message) {
            this.level = level;
            this.message = message;
        }

        public LogBuilder withMeta(Object meta) {
            this.meta = meta;
            return this;
        }

        public void log() {
            LogEntry entry = init(level, message, null, null, meta);
            publisher.publishEvent(entry);
        }
    }

    // Helper methods
    private String formatMessage(String format, Object... args) {
        String result = format;
        for (Object arg : args) {
            int index = result.indexOf("{}");
            if (index == -1) {
                break;
            }
            result = result.substring(0, index) + arg + result.substring(index + 2);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private LogEntry init(LogLevel level, String message, String errorCode, String errMessage, Object meta) {
        return LogEntry.builder()
                .workflowId(workflowId)
                .workflowRunId(runId)
                .nodeKey(nodeKey)
                .level(level)
                .message(message)
                .errorCode(errorCode)
                .errorMessage(errMessage)
                .meta(meta instanceof Map ? (Map<String, Object>) meta :
                      meta != null ? Map.of("data", meta) : null)
                .timestamp(Instant.now())
                .traceId(getTraceId())
                .hierarchy(getHierarchy())
                .userId(userId != null ? userId.toString() : null)
                .build();
    }

    // Helper methods for context data since LogContextManager methods don't exist
    private String getTraceId() {
        // TODO: Implement trace ID retrieval when LogContextManager is available
        return null;
    }

    private String getHierarchy() {
        // TODO: Implement hierarchy retrieval when LogContextManager is available
        return null;
    }
}
