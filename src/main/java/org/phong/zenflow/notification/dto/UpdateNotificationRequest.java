package org.phong.zenflow.notification.dto;

import org.phong.zenflow.notification.enums.NotificationType;

import java.io.Serializable;

/**
 * DTO for updating {@link org.phong.zenflow.notification.infrastructure.persistence.entity.Notification}
 */
public record UpdateNotificationRequest(String message, Boolean isRead, NotificationType type) implements Serializable {
}
