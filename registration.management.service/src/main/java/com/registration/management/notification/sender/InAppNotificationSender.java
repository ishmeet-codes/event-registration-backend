package com.registration.management.notification.sender;

import com.registration.management.notification.entity.Notification;
import com.registration.management.notification.entity.NotificationLog;
import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationStatus;
import com.registration.management.notification.repository.NotificationLogRepository;
import com.registration.management.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationSender implements NotificationChannelSender {

    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) {
        LocalDateTime now = LocalDateTime.now();
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(now);
        notification.setFailureReason(null);
        notificationRepository.save(notification);

        NotificationLog logEntry = NotificationLog.builder()
                .notification(notification)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.SENT)
                .recipientEmail(notification.getRecipient() != null ? notification.getRecipient().getEmail() : null)
                .attempts(notification.getRetryCount() + 1)
                .sentAt(now)
                .build();
        notificationLogRepository.save(logEntry);

        log.debug("In-App notification id={} dispatched to user id={}", notification.getId(), notification.getRecipient().getId());
    }
}
