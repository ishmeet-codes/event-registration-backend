package com.registration.management.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleRequest {

    @NotBlank
    @Size(max = 50)
    private String roleCode;

    @NotBlank
    @Size(max = 50)
    private String roleName;

    @Size(max = 255)
    private String description;

    private Boolean systemRole = false;
    private Boolean active = true;
}
