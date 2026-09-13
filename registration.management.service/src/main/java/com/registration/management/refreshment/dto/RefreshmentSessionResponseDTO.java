package com.registration.management.refreshment.dto;

import com.registration.management.refreshment.enums.RecipientCategory;
import com.registration.management.refreshment.enums.TrackingType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshmentSessionResponseDTO {
    private Long id;
    private Long planId;
    private String planName;
    private String name;
    private Long eventId;
    private String eventName;
    private RecipientCategory recipientCategory;
    private TrackingType trackingType;
    private Integer expectedCount;
    private Integer countDistributed;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer displayOrder;
    private Boolean requireCheckinPresence;
    private Boolean active;
    private List<String> assignedTeamNames;
    private Long eligibleCount;
    private Long presentCount;
    private Long distributedCount;
    private Long pendingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
