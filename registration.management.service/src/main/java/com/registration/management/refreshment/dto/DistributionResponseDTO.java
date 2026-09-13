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
public class DistributionResponseDTO {
    private Long id;
    private Long sessionId;
    private String sessionName;
    private RecipientCategory recipientCategory;
    private Long recipientId;
    private String recipientName;
    private String schoolName;
    private String eventName;
    private DistributionStatus status;
    private Long distributedById;
    private String distributedByName;
    private LocalDateTime distributedAt;
    private String remarks;
    private String correctionReason;
    private Long correctedById;
    private String correctedByName;
    private LocalDateTime correctedAt;
}
