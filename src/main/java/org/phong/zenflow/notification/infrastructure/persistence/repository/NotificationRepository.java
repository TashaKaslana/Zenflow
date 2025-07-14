package org.phong.zenflow.notification.infrastructure.persistence.repository;

import org.phong.zenflow.notification.enums.NotificationType;
import org.phong.zenflow.notification.infrastructure.persistence.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find notifications by user ID
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserId(@Param("userId") UUID userId);

    /**
     * Find notifications by user ID with pagination
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find unread notifications by user ID
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") UUID userId);

    /**
     * Find notifications by user ID and type
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndType(@Param("userId") UUID userId, @Param("type") NotificationType type);

    /**
     * Find notifications by workflow ID
     */
    @Query("SELECT n FROM Notification n WHERE n.workflow.id = :workflowId ORDER BY n.createdAt DESC")
    List<Notification> findByWorkflowId(@Param("workflowId") UUID workflowId);

    /**
     * Count unread notifications by user ID
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") UUID userId);

    /**
     * Mark all notifications as read for a user
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") UUID userId);

    /**
     * Mark notification as read
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :notificationId")
    void markAsRead(@Param("notificationId") UUID notificationId);

    /**
     * Delete old notifications
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Find notifications created after a specific time for a user
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.createdAt > :since ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
