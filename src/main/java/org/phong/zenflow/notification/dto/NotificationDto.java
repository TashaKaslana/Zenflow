package org.phong.zenflow.notification.dto;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.notification.enums.NotificationType;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.notification.infrastructure.persistence.entity.Notification}
 */
public record NotificationDto(@NotNull UUID id, @NotNull UUID userId, UUID workflowId,
                             @NotNull String message, @NotNull Boolean isRead,
                             @NotNull Instant createdAt, @NotNull NotificationType type) implements Serializable {
}
