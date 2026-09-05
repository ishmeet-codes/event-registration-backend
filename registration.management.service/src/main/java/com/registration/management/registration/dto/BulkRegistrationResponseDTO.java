package com.registration.management.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkRegistrationResponseDTO {
    private String submissionId;
    private Long schoolId;
    private int registrationsCreated;
    private int participantsSubmitted;
    private List<BulkRegistrationItemResponseDTO> registrations;
}
