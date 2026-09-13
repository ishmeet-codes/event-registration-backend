package com.registration.management.refreshment.dto;

import com.registration.management.refreshment.enums.DistributionStatus;
import com.registration.management.refreshment.enums.RecipientCategory;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientDistributionDTO {
    private Long recipientId;
    private RecipientCategory recipientCategory;
    private String name;
    private String schoolName;
    private Long eventId;
    private String eventName;
    private String contactNumber;
    private String attendanceStatus; // PRESENT, ABSENT, NOT_REQUIRED
    private String refreshmentStatus; // GIVEN, PENDING, NOT_ELIGIBLE
    private Long distributionId;
    private DistributionStatus distributionState;
    private String distributedByName;
    private LocalDateTime distributedAt;
    private String remarks;
    private Boolean eligible;
}
