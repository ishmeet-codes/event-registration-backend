package com.registration.management.refreshment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrectionRequestDTO {

    @NotBlank(message = "Reason is mandatory for correction")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
