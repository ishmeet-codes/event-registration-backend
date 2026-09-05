package com.registration.management.participant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantEligibilityResponseDTO {

    private Long participantId;
    private boolean eligible;
    private List<String> reasons;
}
