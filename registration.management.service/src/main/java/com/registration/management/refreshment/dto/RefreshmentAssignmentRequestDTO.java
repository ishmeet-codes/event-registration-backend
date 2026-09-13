package com.registration.management.refreshment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshmentAssignmentRequestDTO {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Team ID is required")
    private Long teamId;
}
