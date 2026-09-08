package com.registration.management.notification.dto;

import com.registration.management.notification.enums.NotificationChannel;
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
public class NotificationTemplateResponseDTO {
    private Long id;
    private String code;
    private String name;
    private NotificationType type;
    private NotificationChannel channel;
    private String subject;
    private String body;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
