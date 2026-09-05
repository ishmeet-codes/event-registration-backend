package com.registration.management.registration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkRegistrationRequestDTO {

    @NotNull(message = "School ID is required")
    private Long schoolId;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;

    @Valid
    @NotNull(message = "Participants list is required")
    @NotEmpty(message = "At least one participant is required")
    private List<BulkParticipantDTO> participants;

    @Valid
    @NotNull(message = "Registrations list is required")
    @NotEmpty(message = "At least one event registration is required")
    private List<BulkRegistrationItemRequestDTO> registrations;
}
