package com.registration.management.participant.dto;

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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantCreateDTO {

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

    private List<Long> eventIds;
}
