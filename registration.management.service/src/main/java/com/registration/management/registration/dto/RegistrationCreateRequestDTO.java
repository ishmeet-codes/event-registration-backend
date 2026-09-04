package com.registration.management.registration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationCreateRequestDTO {

    @NotNull(message = "School ID is required")
    private Long schoolId;

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Created by staff ID is required")
    private Long createdByStaffId;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}
