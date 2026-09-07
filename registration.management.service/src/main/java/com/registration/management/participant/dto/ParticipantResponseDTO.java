package com.registration.management.participant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.registration.management.enums.Gender;
import com.registration.management.registration.dto.EventSummaryDTO;
import com.registration.management.registration.dto.SchoolSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParticipantResponseDTO {

    private Long id;
    private Long registrationId;
    private String fullName;
    private String email;
    private Gender gender;
    private String className;
    private LocalDate dob;
    private String guardianPhone;
    private SchoolSummaryDTO school;
    private EventSummaryDTO event;
    private List<EventSummaryDTO> events;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
