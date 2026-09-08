package com.registration.management.notification.dto;

import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateRequestDTO {

    @NotBlank(message = "Template code is required")
    @Size(max = 100, message = "Code must be at most 100 characters")
    private String code;

    @NotBlank(message = "Template name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @NotNull(message = "Template notification type is required")
    private NotificationType type;

    @NotNull(message = "Template channel is required")
    private NotificationChannel channel;

    private String subject;

    @NotBlank(message = "Template body is required")
    private String body;

    @Builder.Default
    private boolean active = true;
}
