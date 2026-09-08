package com.registration.management.notification.event;

import com.registration.management.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void handleDomainNotificationEvent(DomainNotificationEvent event) {
        if (event == null) return;
        try {
            notificationService.sendDomainNotification(event);
        } catch (Exception ex) {
            log.error("Error processing DomainNotificationEvent: {}", ex.getMessage(), ex);
        }
    }
}
