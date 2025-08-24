package org.phong.zenflow.workflow.subdomain.logging.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.phong.zenflow.workflow.subdomain.logging.api.enums.LogLevel;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record NodeLogDto(
        UUID id,
        UUID workflowId,
        UUID workflowRunId,
        String nodeKey,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime timestamp,
        LogLevel level,
        String message,
        String errorCode,
        String errorMessage,
        Map<String, Object> meta,
        String traceId,
        String hierarchy,
        UUID userId,
        String correlationId
) {}
