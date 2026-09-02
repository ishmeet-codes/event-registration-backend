package com.registration.management.school.dto;

import com.registration.management.enums.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class schoolStaffDTO {

    private Long id;

    private Long schoolId;

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name must not exceed 120 characters")
    private String fullName;

    @Size(max = 100, message = "Designation must not exceed 100 characters")
    private String designation;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits")
    private String phone;

    @Email(message = "Invalid email format")
    @Size(max = 120, message = "Email must not exceed 120 characters")
    private String email;

    @NotNull(message = "Staff role is required")
    private StaffRole staffRole;

    private Boolean active;

    private Long createdById;

    private String createdByName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
