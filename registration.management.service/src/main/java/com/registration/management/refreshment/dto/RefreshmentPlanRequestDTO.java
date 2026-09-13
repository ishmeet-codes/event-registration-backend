package com.registration.management.refreshment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshmentPlanRequestDTO {

    @NotBlank(message = "Plan name is required")
    @Size(max = 255)
    private String name;

    @NotNull(message = "Event date is required")
    private LocalDate eventDate;

    private String description;

    @Builder.Default
    private Boolean active = true;
}
