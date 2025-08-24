package org.phong.zenflow.workflow.subdomain.logging.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NodeLogPublisher is responsible for publishing log entries related to workflow nodes.
 * It provides methods to log messages at different levels (success, debug, info, warn, error)
 * and includes metadata such as workflow ID, run ID, node key, and user ID.
 *
 * <p>Usage examples:</p>
 * <pre>
 *     log.withMeta(Map.of("step", "start")).info("Started");
 *     try {
 *         // ...
 *     } catch (Exception e) {
 *         log.withMeta(Map.of("step", "fail"))
 *            .withException(e)
 *            .error("Execution failed");
 *     }
 * </pre>
 */
@AllArgsConstructor
@Builder
@SuppressWarnings("unused")
public class NodeLogPublisher {
    private final ApplicationEventPublisher publisher;
    private final UUID workflowId;
    private final UUID runId;
    @Setter
    private String nodeKey;
    private final UUID userId;

    // mutable builder state
    private Map<String, Object> meta;
    private Throwable exception;

    /**
     * Attach metadata to the next log call.
     */
    public NodeLogPublisher withMeta(Map<String, Object> meta) {
        this.meta = meta;
        return this;
    }

    /**
     * Attach an exception to the next log call. The exception message and stack trace
     * will be captured.
     */
    public NodeLogPublisher withException(Throwable t) {
        this.exception = t;
        return this;
    }

    // Basic logging methods
    public void success(String message) {
        publish(LogLevel.SUCCESS, message);
    }

    public void debug(String message) {
        publish(LogLevel.DEBUG, message);
    }

    public void info(String message) {
        publish(LogLevel.INFO, message);
    }

    public void warn(String message) {
        publish(LogLevel.WARNING, message);
    }

    public void error(String message) {
        publish(LogLevel.ERROR, message);
    }

    // Parameterized logging methods
    public void success(String format, Object... args) {
        publish(LogLevel.SUCCESS, formatMessage(format, args));
    }

    public void debug(String format, Object... args) {
        publish(LogLevel.DEBUG, formatMessage(format, args));
    }

    public void info(String format, Object... args) {
        publish(LogLevel.INFO, formatMessage(format, args));
    }

    public void warn(String format, Object... args) {
        publish(LogLevel.WARNING, formatMessage(format, args));
    }

    public void error(String format, Object... args) {
        publish(LogLevel.ERROR, formatMessage(format, args));
    }

    private void publish(LogLevel level, String message) {
        Map<String, Object> metaToUse = this.meta != null ? new HashMap<>(this.meta) : null;
        String errMessage = null;
        if (exception != null) {
            errMessage = exception.getMessage();
            if (metaToUse == null) {
                metaToUse = new HashMap<>();
            }
            metaToUse.put("stackTrace", getStackTraceAsString(exception));
        }
        LogEntry entry = init(level, message, null, errMessage, metaToUse);
        publisher.publishEvent(entry);
        reset();
    }

    private void reset() {
        this.meta = null;
        this.exception = null;
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

    private String getStackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private LogEntry init(LogLevel level, String message, String errorCode, String errMessage, Map<String, Object> meta) {
        return LogEntry.builder()
                .workflowId(workflowId)
                .workflowRunId(runId)
                .nodeKey(nodeKey)
                .level(level)
                .message(message)
                .errorCode(errorCode)
                .errorMessage(errMessage)
                .meta(meta)
                .timestamp(Instant.now())
                .traceId(getTraceId())
                .hierarchy(getHierarchy())
                .userId(userId)
                .build();
    }

    // Helper methods for context data using LogContextManager
    private String getTraceId() {
        LogContext context = LogContextManager.snapshot();
        return context.traceId();
    }

    private String getHierarchy() {
        LogContext context = LogContextManager.snapshot();
        return context.hierarchy();
    }
}
