package com.registration.management.registration.dto;

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
public class BulkRegistrationItemRequestDTO {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotEmpty(message = "At least one participant client ID is required for each event registration")
    private List<String> participantClientIds;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}
