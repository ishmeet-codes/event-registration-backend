package com.registration.management.notification.dto;

import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationStatus;
import com.registration.management.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {

    private Long id;
    private Long recipientUserId;
    private String recipientName;
    private String recipientEmail;
    private NotificationType type;
    private NotificationChannel channel;
    private String title;
    private String message;
    private NotificationStatus status;
    private boolean read;
    private ReferenceDTO reference;
    private int retryCount;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
