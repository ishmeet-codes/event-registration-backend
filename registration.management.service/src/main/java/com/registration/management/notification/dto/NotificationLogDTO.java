package com.registration.management.notification.dto;

import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogDTO {
    private Long id;
    private Long notificationId;
    private NotificationChannel channel;
    private NotificationStatus status;
    private String recipientEmail;
    private String recipientPhone;
    private int attempts;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private String failureReason;
    private LocalDateTime createdAt;
}
