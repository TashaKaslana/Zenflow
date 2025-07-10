package org.phong.zenflow.log.systemlog.dto;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.log.systemlog.enums.SystemLogType;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.log.systemlog.infrastructure.persistence.entity.SystemLog}
 */
public record SystemLogDto(UUID id, @NotNull String message, Map<String, Object> context,
                           @NotNull SystemLogType logType, @NotNull OffsetDateTime createdAt) implements Serializable {
}