package com.registration.management.notification.repository;

import com.registration.management.notification.entity.NotificationTemplate;
import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long>, JpaSpecificationExecutor<NotificationTemplate> {
    Optional<NotificationTemplate> findByCodeAndDeletedAtIsNull(String code);
    Optional<NotificationTemplate> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByCodeAndDeletedAtIsNull(String code);
    Optional<NotificationTemplate> findByTypeAndChannelAndActiveTrueAndDeletedAtIsNull(NotificationType type, NotificationChannel channel);
}
