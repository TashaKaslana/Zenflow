package org.phong.zenflow.workflow.subdomain.node_logs.dto;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.workflow.subdomain.node_logs.enums.LogLevel;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record CreateNodeLogRequest(
        @NotNull UUID workflowId,
        @NotNull UUID workflowRunId,
        @NotNull String nodeKey,
        OffsetDateTime timestamp,
        @NotNull LogLevel level,
        String message,
        String errorCode,
        String errorMessage,
        Map<String, Object> meta,
        String traceId,
        String hierarchy,
        UUID userId,
        String correlationId
) {}
