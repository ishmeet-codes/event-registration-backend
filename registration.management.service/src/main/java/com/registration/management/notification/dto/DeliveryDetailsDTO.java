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
public class DeliveryDetailsDTO {
    private Long notificationId;
    private NotificationChannel channel;
    private NotificationStatus status;
    private int attempts;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private String failureReason;
    private String recipientEmail;
    private String recipientPhone;
}
