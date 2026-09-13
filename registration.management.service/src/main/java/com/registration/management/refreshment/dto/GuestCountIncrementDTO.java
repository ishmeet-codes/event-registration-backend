package com.registration.management.refreshment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestCountIncrementDTO {

    @NotNull(message = "Increment count is required")
    @Min(value = 1, message = "Increment count must be at least 1")
    private Integer incrementBy;

    @Size(max = 500)
    private String remarks;
}
