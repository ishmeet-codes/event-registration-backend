package com.registration.management.notification.dto;

import com.registration.management.notification.enums.CampaignStatus;
import com.registration.management.notification.enums.RecipientType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Long templateId;
    private Integer templateVersion;
    private CampaignStatus status;
    private RecipientType audienceType;
    private AudienceCriteriaDTO audienceCriteria;

    private Integer totalRecipients;
    private Integer validRecipientsCount;
    private Integer sentCount;
    private Integer failedCount;
    private Integer deliveredCount;
    private Integer bouncedCount;

    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastErrorMessage;
}
