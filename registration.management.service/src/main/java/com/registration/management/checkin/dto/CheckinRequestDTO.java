package com.registration.management.checkin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinRequestDTO {

    @NotNull(message = "Participant ID is required")
    private Long participantId;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}
