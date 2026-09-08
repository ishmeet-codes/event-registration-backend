package com.registration.management.notification.repository;

import com.registration.management.notification.entity.Notification;
import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationStatus;
import com.registration.management.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    Optional<Notification> findByIdAndDeletedAtIsNull(Long id);

    Optional<Notification> findByIdAndRecipientIdAndDeletedAtIsNull(Long id, Long recipientId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :recipientId AND n.readAt IS NULL AND n.status <> 'READ' AND n.deletedAt IS NULL")
    long countUnreadByRecipientId(@Param("recipientId") Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now, n.status = 'READ', n.updatedAt = :now WHERE n.recipient.id = :recipientId AND n.readAt IS NULL AND n.deletedAt IS NULL")
    int markAllAsReadForRecipient(@Param("recipientId") Long recipientId, @Param("now") LocalDateTime now);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.scheduledAt IS NOT NULL AND n.scheduledAt <= :now AND n.deletedAt IS NULL")
    List<Notification> findReadyScheduledNotifications(@Param("now") LocalDateTime now);

    boolean existsByIdempotencyKeyAndStatusNot(String idempotencyKey, NotificationStatus status);

    boolean existsByRecipientIdAndTypeAndReferenceTypeAndReferenceIdAndChannelAndStatusIn(
            Long recipientId,
            NotificationType type,
            String referenceType,
            Long referenceId,
            NotificationChannel channel,
            List<NotificationStatus> statuses
    );

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.deletedAt IS NULL AND (:dateFrom IS NULL OR n.createdAt >= :dateFrom) AND (:dateTo IS NULL OR n.createdAt <= :dateTo) AND (:channel IS NULL OR n.channel = :channel) AND (:type IS NULL OR n.type = :type)")
    long countSummaryTotal(
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("channel") NotificationChannel channel,
            @Param("type") NotificationType type
    );

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status AND n.deletedAt IS NULL AND (:dateFrom IS NULL OR n.createdAt >= :dateFrom) AND (:dateTo IS NULL OR n.createdAt <= :dateTo) AND (:channel IS NULL OR n.channel = :channel) AND (:type IS NULL OR n.type = :type)")
    long countSummaryByStatus(
            @Param("status") NotificationStatus status,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("channel") NotificationChannel channel,
            @Param("type") NotificationType type
    );

    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.status = 'READ' OR n.readAt IS NOT NULL) AND n.deletedAt IS NULL AND (:dateFrom IS NULL OR n.createdAt >= :dateFrom) AND (:dateTo IS NULL OR n.createdAt <= :dateTo) AND (:channel IS NULL OR n.channel = :channel) AND (:type IS NULL OR n.type = :type)")
    long countSummaryRead(
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("channel") NotificationChannel channel,
            @Param("type") NotificationType type
    );

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = 'PENDING' AND n.scheduledAt > :now AND n.deletedAt IS NULL AND (:dateFrom IS NULL OR n.createdAt >= :dateFrom) AND (:dateTo IS NULL OR n.createdAt <= :dateTo) AND (:channel IS NULL OR n.channel = :channel) AND (:type IS NULL OR n.type = :type)")
    long countSummaryScheduled(
            @Param("now") LocalDateTime now,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("channel") NotificationChannel channel,
            @Param("type") NotificationType type
    );
}
