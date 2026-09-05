package com.registration.management.registration.dto;

import com.registration.management.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkParticipantDTO {

    @NotBlank(message = "Client ID is required for participant reference")
    @Size(max = 50, message = "Client ID cannot exceed 50 characters")
    private String clientId;

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name cannot exceed 120 characters")
    private String fullName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Size(max = 20, message = "Class name cannot exceed 20 characters")
    private String className;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @NotBlank(message = "Guardian phone is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Guardian phone must be 10-15 digits")
    private String guardianPhone;
}
