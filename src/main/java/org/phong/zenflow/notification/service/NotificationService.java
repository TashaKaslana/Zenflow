package org.phong.zenflow.notification.service;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.notification.dto.CreateNotificationRequest;
import org.phong.zenflow.notification.dto.NotificationDto;
import org.phong.zenflow.notification.dto.UpdateNotificationRequest;
import org.phong.zenflow.notification.enums.NotificationType;
import org.phong.zenflow.notification.exception.NotificationException;
import org.phong.zenflow.notification.infrastructure.mapstruct.NotificationMapper;
import org.phong.zenflow.notification.infrastructure.persistence.entity.Notification;
import org.phong.zenflow.notification.infrastructure.persistence.repository.NotificationRepository;
import org.phong.zenflow.user.infrastructure.persistence.entities.User;
import org.phong.zenflow.user.infrastructure.persistence.repositories.UserRepository;
import org.phong.zenflow.user.exception.UserNotFoundException;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.exception.WorkflowException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WorkflowRepository workflowRepository;
    private final NotificationMapper notificationMapper;

    /**
     * Create a new notification
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_CREATE, targetIdExpression = "returnObject.id", description = "Create notification")
    public NotificationDto createNotification(CreateNotificationRequest request) {
        // Validate user exists
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId().toString()));

        // Validate workflow exists if provided
        Workflow workflow = null;
        if (request.workflowId() != null) {
            workflow = workflowRepository.findById(request.workflowId())
                    .orElseThrow(() -> new WorkflowException("Workflow not found with id: " + request.workflowId()));
        }

        Notification notification = notificationMapper.toEntity(request);
        notification.setUser(user);
        notification.setWorkflow(workflow);
        notification.setIsRead(false);
        notification.setCreatedAt(Instant.now());

        Notification savedNotification = notificationRepository.save(notification);
        return notificationMapper.toDto(savedNotification);
    }

    /**
     * Create multiple notifications in bulk
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_CREATE, description = "Create bulk notifications")
    public List<NotificationDto> createNotifications(List<CreateNotificationRequest> requests) {
        return requests.stream()
                .map(this::createNotification)
                .toList();
    }

    /**
     * Find notification by ID
     */
    public NotificationDto findById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationException("Notification not found with id: " + id));
        return notificationMapper.toDto(notification);
    }

    /**
     * Find all notifications
     */
    public List<NotificationDto> findAll() {
        return notificationRepository.findAll()
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    /**
     * Find notifications with pagination
     */
    public Page<NotificationDto> findAll(Pageable pageable) {
        return notificationRepository.findAll(pageable)
                .map(notificationMapper::toDto);
    }

    /**
     * Find notifications by user ID
     */
    public List<NotificationDto> findByUserId(UUID userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }

        return notificationRepository.findByUserId(userId)
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    /**
     * Find notifications by user ID with pagination
     */
    public Page<NotificationDto> findByUserId(UUID userId, Pageable pageable) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }

        return notificationRepository.findByUserId(userId, pageable)
                .map(notificationMapper::toDto);
    }

    /**
     * Find unread notifications by user ID
     */
    public List<NotificationDto> findUnreadByUserId(UUID userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }

        return notificationRepository.findUnreadByUserId(userId)
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    /**
     * Find notifications by user ID and type
     */
    public List<NotificationDto> findByUserIdAndType(UUID userId, NotificationType type) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }

        return notificationRepository.findByUserIdAndType(userId, type)
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    /**
     * Find notifications by workflow ID
     */
    public List<NotificationDto> findByWorkflowId(UUID workflowId) {
        // Validate workflow exists
        if (!workflowRepository.existsById(workflowId)) {
            throw new WorkflowException("Workflow not found with id: " + workflowId);
        }

        return notificationRepository.findByWorkflowId(workflowId)
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    /**
     * Find notifications created after a specific time for a user
     */
    public List<NotificationDto> findByUserIdSince(UUID userId, Instant since) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }

        return notificationRepository.findByUserIdSince(userId, since)
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    /**
     * Update notification
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_UPDATE, targetIdExpression = "#id", description = "Update notification")
    public NotificationDto updateNotification(UUID id, UpdateNotificationRequest request) {
        Notification existingNotification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationException("Notification not found with id: " + id));

        Notification updated = notificationMapper.partialUpdate(request, existingNotification);
        Notification updatedNotification = notificationRepository.save(updated);
        return notificationMapper.toDto(updatedNotification);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_UPDATE, targetIdExpression = "#id", description = "Mark notification as read")
    public void markAsRead(UUID id) {
        if (!notificationRepository.existsById(id)) {
            throw new NotificationException("Notification not found with id: " + id);
        }
        notificationRepository.markAsRead(id);
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_UPDATE, targetIdExpression = "#userId", description = "Mark all notifications as read")
    public int markAllAsReadByUserId(UUID userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    /**
     * Count unread notifications by user ID
     */
    public long countUnreadByUserId(UUID userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * Delete notification
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_DELETE, targetIdExpression = "#id", description = "Delete notification")
    public void deleteNotification(UUID id) {
        if (!notificationRepository.existsById(id)) {
            throw new NotificationException("Notification not found with id: " + id);
        }
        notificationRepository.deleteById(id);
    }

    /**
     * Delete old notifications before a specific date
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_DELETE, description = "Delete old notifications")
    public int deleteOldNotifications(Instant cutoffDate) {
        return notificationRepository.deleteOldNotifications(cutoffDate);
    }

    /**
     * Check if notification exists
     */
    public boolean existsById(UUID id) {
        return notificationRepository.existsById(id);
    }
}
