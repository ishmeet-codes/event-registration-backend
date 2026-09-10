package com.registration.management.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.registration.management.notification.enums.RecipientType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequestDTO {

    @NotBlank
    private String title;

    private String message;
    private String channel;
    private String type;

    private RecipientType recipientType;
    private AudienceCriteriaDTO audienceCriteria;
    private List<CustomRecipientDTO> customRecipients;

    private Long templateId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime scheduledAt;
}

