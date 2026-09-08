package com.registration.management.notification.repository;

import com.registration.management.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>, JpaSpecificationExecutor<NotificationLog> {
    Optional<NotificationLog> findTopByNotificationIdOrderByCreatedAtDesc(Long notificationId);
    List<NotificationLog> findByNotificationIdOrderByCreatedAtDesc(Long notificationId);
}
