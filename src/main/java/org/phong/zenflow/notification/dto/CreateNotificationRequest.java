package org.phong.zenflow.notification.dto;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.notification.enums.NotificationType;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for creating {@link org.phong.zenflow.notification.infrastructure.persistence.entity.Notification}
 */
public record CreateNotificationRequest(@NotNull UUID userId, UUID workflowId,
                                       @NotNull String message, @NotNull NotificationType type) implements Serializable {
}
