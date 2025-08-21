package org.phong.zenflow.workflow.subdomain.node_logs.logging;

import lombok.Builder;
import lombok.Data;
import org.phong.zenflow.workflow.subdomain.node_logs.enums.LogLevel;

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
    String userId;          // optional actor
    String correlationId;   // optional cross-service

    public static LogEntry of(LogLevel level, String message) {
        return LogEntry.builder()
                .level(level)
                .message(message)
                .timestamp(Instant.now())
                .traceId(LogContextManager.snapshot().traceId())
                .hierarchy(LogContextManager.snapshot().hierarchy())
                .build();
    }
}
