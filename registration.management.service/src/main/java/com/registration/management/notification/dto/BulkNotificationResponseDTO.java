package com.registration.management.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationResponseDTO {
    private int requestedCount;
    private int queuedCount;
    private int failedCount;
    private List<Long> notificationIds;
    private String message;
}
