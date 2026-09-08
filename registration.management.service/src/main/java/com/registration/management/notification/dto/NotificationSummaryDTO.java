package com.registration.management.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummaryDTO {
    private long total;
    private long sent;
    private long delivered;
    private long failed;
    private long read;
    private long pending;
    private long scheduled;
    private long cancelled;
}
