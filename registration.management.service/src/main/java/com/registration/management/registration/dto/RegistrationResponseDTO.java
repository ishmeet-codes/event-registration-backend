package com.registration.management.registration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.registration.management.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistrationResponseDTO {

    private Long id;
    private SchoolSummaryDTO school;
    private EventSummaryDTO event;
    private StaffSummaryDTO createdByStaff;
    private RegistrationStatus status;
    private String remarks;
    private Long participantCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
