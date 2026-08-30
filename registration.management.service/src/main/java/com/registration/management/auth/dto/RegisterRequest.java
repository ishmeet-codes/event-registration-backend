package com.registration.management.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    private String password;

    /**
     * Role the user is registering as. Only self-service roles are permitted.
     * Defaults to ATTENDEE when not supplied.
     */
    @Pattern(
            regexp = "^(SCHOOL_INCHARGE|PARTICIPANT)$",
            message = "Role must be either SCHOOL_INCHARGE or PARTICIPANT"
    )
    private String roleCode = "PARTICIPANT";
}
