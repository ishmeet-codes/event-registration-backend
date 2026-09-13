package com.registration.management.refreshment.dto;

import com.registration.management.refreshment.enums.RecipientCategory;
import com.registration.management.refreshment.enums.TrackingType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamSessionSummaryDTO {
    private Long sessionId;
    private String sessionName;
    private Long eventId;
    private String eventName;
    private RecipientCategory recipientCategory;
    private TrackingType trackingType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer displayOrder;
    private Long eligibleCount;
    private Long presentCount;
    private Long distributedCount;
    private Long pendingCount;
    private Integer expectedCount;
    private Integer countDistributed;
}
