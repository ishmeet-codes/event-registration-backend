package com.registration.management.refreshment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestRecordRequestDTO {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotBlank(message = "Guest name is required")
    @Size(max = 255)
    private String name;

    private String designation;

    private String organization;

    private String contactNumber;

    private String remarks;
}
