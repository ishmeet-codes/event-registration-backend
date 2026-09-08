package com.registration.management.notification.sender;

import com.registration.management.notification.entity.Notification;
import com.registration.management.notification.enums.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationSenderDispatcher {

    private final Map<NotificationChannel, NotificationChannelSender> senders = new EnumMap<>(NotificationChannel.class);

    public NotificationSenderDispatcher(List<NotificationChannelSender> senderList) {
        for (NotificationChannelSender sender : senderList) {
            senders.put(sender.getChannel(), sender);
        }
    }

    public void dispatch(Notification notification) {
        if (notification == null || notification.getChannel() == null) {
            return;
        }
        NotificationChannelSender sender = senders.get(notification.getChannel());
        if (sender != null) {
            sender.send(notification);
        } else {
            throw new IllegalArgumentException("Unsupported notification channel: " + notification.getChannel());
        }
    }
}
