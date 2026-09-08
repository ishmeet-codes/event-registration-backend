package com.registration.management.notification.dto;

import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationType;
import com.registration.management.notification.enums.RecipientTargetType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationRequestDTO {

    private List<Long> recipientUserIds;

    private RecipientTargetType targetType;

    private Long targetId;

    @Builder.Default
    private NotificationType type = NotificationType.ANNOUNCEMENT;

    @NotNull(message = "Notification channel is required")
    private NotificationChannel channel;

    private String title;

    private String message;

    private String templateCode;

    private Map<String, Object> templateVariables;
}
