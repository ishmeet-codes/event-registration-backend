package com.registration.management.participant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantCountResponseDTO {

    private Long registrationId;
    private long participantCount;
    private int minimumParticipants;
    private int maximumParticipants;
}
