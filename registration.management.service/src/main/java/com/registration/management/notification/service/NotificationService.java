package com.registration.management.notification.service;

import com.registration.management.auth.entities.User;
import com.registration.management.notification.dto.*;
import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationStatus;
import com.registration.management.notification.enums.NotificationType;
import com.registration.management.notification.event.DomainNotificationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Map;

public interface NotificationService {

    // 1. Create Notification
    NotificationResponseDTO createNotification(NotificationRequestDTO request, User currentUser);

    // 2. List Notifications (Inbox / Management)
    Page<NotificationResponseDTO> getNotifications(
            String search,
            NotificationType type,
            NotificationChannel channel,
            NotificationStatus status,
            Boolean read,
            Long recipientUserId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable,
            User currentUser
    );

    // 3. Notification Details
    NotificationResponseDTO getNotificationById(Long id, User currentUser);

    // 4. Mark as Read
    NotificationResponseDTO markAsRead(Long id, User currentUser);

    // 5. Mark All Read
    Map<String, Object> markAllAsRead(User currentUser);

    // 6. Unread Count Badge
    UnreadCountDTO getUnreadCount(User currentUser);

    // 7. Delete Notification (Soft Delete)
    void deleteNotification(Long id, User currentUser);

    // 8. Bulk Notification
    BulkNotificationResponseDTO sendBulkNotification(BulkNotificationRequestDTO request, User currentUser);

    // 9. Schedule Notification
    BulkNotificationResponseDTO scheduleNotification(ScheduleNotificationRequestDTO request, User currentUser);

    // 10. Cancel Scheduled Notification
    void cancelScheduledNotification(Long id, User currentUser);

    // 11. Scheduled Notifications List
    Page<NotificationResponseDTO> getScheduledNotifications(
            NotificationChannel channel,
            NotificationStatus status,
            String templateCode,
            LocalDateTime scheduledFrom,
            LocalDateTime scheduledTo,
            Pageable pageable,
            User currentUser
    );

    // 12. Delivery Details
    DeliveryDetailsDTO getDeliveryDetails(Long id, User currentUser);

    // 13. Retry Failed Notification
    NotificationResponseDTO retryNotification(Long id, User currentUser);

    // 14. List Templates
    Page<NotificationTemplateResponseDTO> getTemplates(
            String search,
            NotificationType type,
            NotificationChannel channel,
            Boolean active,
            Pageable pageable
    );

    // 15. Create Template
    NotificationTemplateResponseDTO createTemplate(NotificationTemplateRequestDTO request, User currentUser);

    // 16. Template Details
    NotificationTemplateResponseDTO getTemplateById(Long id);

    // 17. Update Template
    NotificationTemplateResponseDTO updateTemplate(Long id, NotificationTemplateRequestDTO request, User currentUser);

    // 18. Delete Template
    void deleteTemplate(Long id, User currentUser);

    // 19. Update Template Status (Activate/Deactivate)
    NotificationTemplateResponseDTO updateTemplateStatus(Long id, TemplateStatusUpdateRequestDTO request, User currentUser);

    // 20. Get Own Preferences
    NotificationPreferenceDTO getPreferences(User currentUser);

    // 21. Update Own Preferences
    NotificationPreferenceDTO updatePreferences(NotificationPreferenceDTO request, User currentUser);

    // 22. Notification Summary / Analytics
    NotificationSummaryDTO getSummary(
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            NotificationChannel channel,
            NotificationType type
    );

    // 23. Delivery / Notification Logs
    Page<NotificationLogDTO> getLogs(
            NotificationStatus status,
            NotificationChannel channel,
            NotificationType type,
            String recipient,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    );

    // Background & Internal Processing
    void sendDomainNotification(DomainNotificationEvent event);
    void processScheduledNotifications();
}
