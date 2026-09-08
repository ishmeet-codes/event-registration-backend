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
public class SmsNotificationSender implements NotificationChannelSender {

    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(Notification notification) {
        // SMS channel placeholder as specified in requirements
        log.info("SMS channel triggered for notification id={}. SMS gateway provider not yet integrated.", notification.getId());
        LocalDateTime now = LocalDateTime.now();
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(now);
        notificationRepository.save(notification);

        NotificationLog logEntry = NotificationLog.builder()
                .notification(notification)
                .channel(NotificationChannel.SMS)
                .status(NotificationStatus.SENT)
                .attempts(notification.getRetryCount() + 1)
                .sentAt(now)
                .build();
        notificationLogRepository.save(logEntry);
    }
}
