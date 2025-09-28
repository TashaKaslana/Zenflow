package org.phong.zenflow.workflow.subdomain.logging.core;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
@Data
public class LogEntry {
    UUID workflowId;
    UUID workflowRunId;
    String nodeKey;
    Instant timestamp;
    LogLevel level;         // TRACE/DEBUG/INFO/WARN/ERROR
    String message;
    String errorCode;       // optional classification
    String errorMessage;    // optional human-readable message
    Map<String, Object> meta;// optional (JSONB in DB)
    String traceId;         // from LogContext/MDC
    String hierarchy;       // from LogContext (a->b->c)
    UUID userId;          // optional actor
    String correlationId;   // optional cross-service

    public static LogEntry of(LogLevel level, String message) {
        final var context = LogContextManager.snapshot();
        return LogEntry.builder()
                .level(level)
                .message(message)
                .timestamp(Instant.now())
                .traceId(context.traceId())
                .hierarchy(context.hierarchy())
                .build();
    }
}
