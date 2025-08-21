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
 * <p>
 * Usage examples:
 * - Simple: nodeLogger.info("User logged in");
 * - Parameterized: nodeLogger.info("User {} logged in", username);
 * - With metadata: nodeLogger.infoBuilder("User {} logged in", username).withMeta(loginData).log();
 * - Error with context: nodeLogger.error("DB failed").withErrorCode("DB_001").log();
 */
@AllArgsConstructor
@Builder
@SuppressWarnings("unused")
public class NodeLogPublisher {
    private final ApplicationEventPublisher publisher;

    private final UUID workflowId;
    private final UUID workflowRunId;
    private String nodeKey;
    private UUID userId;

    // SLF4J-style logging methods that log immediately
    public void success(String message) {
        LogEntry success = init(LogLevel.SUCCESS, message);
        publisher.publishEvent(success);
    }

    public void debug(String message) {
        LogEntry debug = init(LogLevel.DEBUG, message);
        publisher.publishEvent(debug);
    }

    public void info(String message) {
        LogEntry info = init(LogLevel.INFO, message);
        publisher.publishEvent(info);
    }

    public void warn(String message) {
        LogEntry warn = init(LogLevel.WARNING, message);
        publisher.publishEvent(warn);
    }

    // SLF4J-style parameterized logging methods that log immediately
    public void success(String format, Object... args) {
        String formattedMessage = formatMessage(format, args);
        LogEntry success = init(LogLevel.SUCCESS, formattedMessage);
        publisher.publishEvent(success);
    }

    public void debug(String format, Object... args) {
        String formattedMessage = formatMessage(format, args);
        LogEntry debug = init(LogLevel.DEBUG, formattedMessage);
        publisher.publishEvent(debug);
    }

    public void info(String format, Object... args) {
        String formattedMessage = formatMessage(format, args);
        LogEntry info = init(LogLevel.INFO, formattedMessage);
        publisher.publishEvent(info);
    }

    public void warn(String format, Object... args) {
        String formattedMessage = formatMessage(format, args);
        LogEntry warn = init(LogLevel.WARNING, formattedMessage);
        publisher.publishEvent(warn);
    }

    // Builder methods for advanced cases (metadata, error context)
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

    public LogBuilder successBuilder(String format, Object... args) {
        return new LogBuilder(LogLevel.SUCCESS, formatMessage(format, args));
    }

    public LogBuilder debugBuilder(String format, Object... args) {
        return new LogBuilder(LogLevel.DEBUG, formatMessage(format, args));
    }

    public LogBuilder infoBuilder(String format, Object... args) {
        return new LogBuilder(LogLevel.INFO, formatMessage(format, args));
    }

    public LogBuilder warnBuilder(String format, Object... args) {
        return new LogBuilder(LogLevel.WARNING, formatMessage(format, args));
    }

    // Error methods - always return builder for rich error context
    public ErrorLogBuilder error(String message) {
        return new ErrorLogBuilder(message);
    }

    public ErrorLogBuilder error(String format, Object... args) {
        return new ErrorLogBuilder(formatMessage(format, args));
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
            LogEntry entry = init(level, message, meta);
            publisher.publishEvent(entry);
        }
    }

    /**
     * Fluent builder for error log entries
     */
    public class ErrorLogBuilder {
        private final String message;
        private String errorCode;
        private String errorMessage;
        private Object meta;

        private ErrorLogBuilder(String message) {
            this.message = message;
        }

        public ErrorLogBuilder withErrorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public ErrorLogBuilder withErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ErrorLogBuilder withMeta(Object meta) {
            this.meta = meta;
            return this;
        }

        public void log() {
            LogEntry entry = init(LogLevel.ERROR, message, errorCode, errorMessage, meta);
            publisher.publishEvent(entry);
        }
    }

    /**
     * Formats a message string with {} placeholders using provided arguments.
     * This mimics SLF4J's message formatting behavior.
     *
     * @param format The format string with {} placeholders
     * @param args   The arguments to substitute into the placeholders
     * @return The formatted message string
     */
    private String formatMessage(String format, Object... args) {
        if (format == null || args == null || args.length == 0) {
            return format;
        }

        String result = format;
        for (Object arg : args) {
            int index = result.indexOf("{}");
            if (index == -1) {
                break; // No more placeholders
            }
            String replacement = arg != null ? arg.toString() : "null";
            result = result.substring(0, index) + replacement + result.substring(index + 2);
        }

        return result;
    }

    private LogEntry init(LogLevel level, String message) {
        return LogEntry.builder()
                .workflowId(workflowId)
                .workflowRunId(workflowRunId)
                .nodeKey(nodeKey)
                .level(level)
                .timestamp(Instant.now())
                .message(message)
                .traceId(LogContextManager.snapshot().traceId())
                .hierarchy(LogContextManager.snapshot().hierarchy())
                .userId(userId != null ? userId.toString() : null)
                .build();
    }

    private LogEntry init(LogLevel level, String message, Object meta) {
        return LogEntry.builder()
                .workflowId(workflowId)
                .workflowRunId(workflowRunId)
                .nodeKey(nodeKey)
                .level(level)
                .timestamp(Instant.now())
                .message(message)
                .meta(meta != null ? Map.of("meta", meta) : null)
                .traceId(LogContextManager.snapshot().traceId())
                .hierarchy(LogContextManager.snapshot().hierarchy())
                .userId(userId != null ? userId.toString() : null)
                .build();
    }

    private LogEntry init(LogLevel level, String message, String errorCode, String errMessage, Object meta) {
        return LogEntry.builder()
                .workflowId(workflowId)
                .workflowRunId(workflowRunId)
                .nodeKey(nodeKey)
                .level(level)
                .timestamp(Instant.now())
                .message(message)
                .errorCode(errorCode)
                .errorMessage(errMessage)
                .meta(meta != null ? Map.of("meta", meta) : null)
                .traceId(LogContextManager.snapshot().traceId())
                .hierarchy(LogContextManager.snapshot().hierarchy())
                .userId(userId != null ? userId.toString() : null)
                .build();
    }
}
