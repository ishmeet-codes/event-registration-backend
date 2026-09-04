package com.registration.management.registration.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationUpdateRequestDTO {

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}
