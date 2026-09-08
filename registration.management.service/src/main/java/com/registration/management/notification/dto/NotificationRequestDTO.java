package com.registration.management.notification.dto;

import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDTO {

    @NotNull(message = "recipientUserId is required")
    private Long recipientUserId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Notification channel is required")
    private NotificationChannel channel;

    private String title;

    private String message;

    private String templateCode;

    private Map<String, Object> templateVariables;

    private String referenceType;

    private Long referenceId;

    private String idempotencyKey;
}
