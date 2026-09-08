package com.registration.management.notification.sender;

import com.registration.management.notification.entity.Notification;
import com.registration.management.notification.enums.NotificationChannel;

public interface NotificationChannelSender {
    NotificationChannel getChannel();
    void send(Notification notification);
}
