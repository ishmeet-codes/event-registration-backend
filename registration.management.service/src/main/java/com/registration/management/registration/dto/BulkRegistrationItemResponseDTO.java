package com.registration.management.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkRegistrationItemResponseDTO {
    private Long registrationId;
    private Long eventId;
    private String eventName;
    private int participantCount;
}
