package com.registration.management.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDTO {
    private Long id;
    private Long userId;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean inAppEnabled;
    private boolean eventEnabled;
    private boolean registrationEnabled;
    private boolean checkinEnabled;
    private LocalDateTime updatedAt;
}
