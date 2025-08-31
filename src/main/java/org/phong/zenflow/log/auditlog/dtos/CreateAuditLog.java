package org.phong.zenflow.log.auditlog.dtos;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.log.auditlog.infrastructure.persistence.entity.AuditLogEntity;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link AuditLogEntity}
 */
public record CreateAuditLog(@NotNull UUID userId, @NotNull String action, @NotNull String targetType,
                             UUID targetId, String description,
                             Map<String, Object> metadata, String userAgent,
                             String ipAddress) implements Serializable {
}