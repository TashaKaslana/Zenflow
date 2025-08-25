package org.phong.zenflow.workflow.subdomain.logging.api.dto;

import org.phong.zenflow.workflow.subdomain.logging.api.enums.LogLevel;

import java.util.Map;
import java.util.UUID;

public record UpdateNodeLogRequest(
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
