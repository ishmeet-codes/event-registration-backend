package com.registration.management.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class schoolDTO {

    private Long id;

    @NotBlank(message = "School code is required")
    @Size(max = 50, message = "School code must not exceed 50 characters")
    private String schoolCode;

    @NotBlank(message = "School name is required")
    @Size(max = 150, message = "School name must not exceed 150 characters")
    private String schoolName;

    @Size(max = 120, message = "Principal name must not exceed 120 characters")
    private String principalName;

    @Size(max = 50, message = "Board must not exceed 50 characters")
    private String board;

    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits")
    private String phone;

    @Email(message = "Invalid email format")
    @Size(max = 120, message = "Email must not exceed 120 characters")
    private String email;

    private Boolean active;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
