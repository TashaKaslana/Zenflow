package org.phong.zenflow.notification.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.notification.dto.CreateNotificationRequest;
import org.phong.zenflow.notification.dto.NotificationDto;
import org.phong.zenflow.notification.dto.UpdateNotificationRequest;
import org.phong.zenflow.notification.enums.NotificationType;
import org.phong.zenflow.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<RestApiResponse<NotificationDto>> createNotification(@Valid @RequestBody CreateNotificationRequest request) {
        NotificationDto createdNotification = notificationService.createNotification(request);
        return RestApiResponse.created(createdNotification, "Notification created successfully");
    }

    @PostMapping("/bulk")
    public ResponseEntity<RestApiResponse<List<NotificationDto>>> createNotifications(@Valid @RequestBody List<CreateNotificationRequest> requests) {
        List<NotificationDto> createdNotifications = notificationService.createNotifications(requests);
        return RestApiResponse.created(createdNotifications, "Notifications created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<NotificationDto>> getNotificationById(@PathVariable UUID id) {
        NotificationDto notification = notificationService.findById(id);
        return RestApiResponse.success(notification, "Notification retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<NotificationDto>>> getAllNotifications() {
        List<NotificationDto> notifications = notificationService.findAll();
        return RestApiResponse.success(notifications, "Notifications retrieved successfully");
    }

    @GetMapping("/paginated")
    public ResponseEntity<RestApiResponse<List<NotificationDto>>> getAllNotificationsPaginated(Pageable pageable) {
        Page<NotificationDto> notifications = notificationService.findAll(pageable);
        return RestApiResponse.success(notifications, "Notifications retrieved successfully");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<RestApiResponse<List<NotificationDto>>> getNotificationsByUserId(@PathVariable UUID userId) {
        List<NotificationDto> notifications = notificationService.findByUserId(userId);
        return RestApiResponse.success(notifications, "User notifications retrieved successfully");
    }

    @GetMapping("/user/{userId}/paginated")
    public ResponseEntity<RestApiResponse<List<NotificationDto>>> getNotificationsByUserIdPaginated(
            @PathVariable UUID userId, Pageable pageable) {
        Page<NotificationDto> notifications = notificationService.findByUserId(userId, pageable);
        return RestApiResponse.success(notifications, "User notifications retrieved successfully");
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<RestApiResponse<List<NotificationDto>>> getUnreadNotificationsByUserId(@PathVariable UUID userId) {
        List<NotificationDto> notifications = notificationService.findUnreadByUserId(userId);
        return RestApiResponse.success(notifications, "Unread notifications retrieved successfully");
    }

    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<RestApiResponse<List<NotificationDto>>> getNotificationsByUserIdAndType(
            @PathVariable UUID userId, @PathVariable NotificationType type) {
        List<NotificationDto> notifications = notificationService.findByUserIdAndType(userId, type);
        return RestApiResponse.success(notifications, "Notifications by type retrieved successfully");
    }

    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<RestApiResponse<List<NotificationDto>>> getNotificationsByWorkflowId(@PathVariable UUID workflowId) {
        List<NotificationDto> notifications = notificationService.findByWorkflowId(workflowId);
        return RestApiResponse.success(notifications, "Workflow notifications retrieved successfully");
    }

    @GetMapping("/user/{userId}/since")
    public ResponseEntity<RestApiResponse<List<NotificationDto>>> getNotificationsByUserIdSince(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        List<NotificationDto> notifications = notificationService.findByUserIdSince(userId, since);
        return RestApiResponse.success(notifications, "Recent notifications retrieved successfully");
    }

    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<RestApiResponse<Long>> getUnreadNotificationCountByUserId(@PathVariable UUID userId) {
        long count = notificationService.countUnreadByUserId(userId);
        return RestApiResponse.success(count, "Unread notification count retrieved successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<NotificationDto>> updateNotification(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNotificationRequest request) {
        NotificationDto updatedNotification = notificationService.updateNotification(id, request);
        return RestApiResponse.success(updatedNotification, "Notification updated successfully");
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<RestApiResponse<Void>> markNotificationAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return RestApiResponse.success("Notification marked as read successfully");
    }

    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<RestApiResponse<Integer>> markAllNotificationsAsReadByUserId(@PathVariable UUID userId) {
        int updatedCount = notificationService.markAllAsReadByUserId(userId);
        return RestApiResponse.success(updatedCount, "All notifications marked as read");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deleteNotification(@PathVariable UUID id) {
        notificationService.deleteNotification(id);
        return RestApiResponse.noContent();
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<RestApiResponse<Integer>> deleteOldNotifications(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cutoffDate) {
        int deletedCount = notificationService.deleteOldNotifications(cutoffDate);
        return RestApiResponse.success(deletedCount, "Old notifications deleted successfully");
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<RestApiResponse<Boolean>> checkNotificationExists(@PathVariable UUID id) {
        boolean exists = notificationService.existsById(id);
        return RestApiResponse.success(exists, "Notification existence checked");
    }
}
