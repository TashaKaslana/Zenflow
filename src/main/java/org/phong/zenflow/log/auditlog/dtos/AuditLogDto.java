package org.phong.zenflow.log.auditlog.dtos;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.log.auditlog.infrastructure.persistence.entity.AuditLogEntity;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link AuditLogEntity}
 */
public record AuditLogDto(UUID id, @NotNull UUID userId, @NotNull String action,
                          @NotNull String targetType, @NotNull UUID targetId,
                          String description, Map<String, Object> metadata, String userAgent, String ipAddress,
                          @NotNull OffsetDateTime createdAt) implements Serializable {
}