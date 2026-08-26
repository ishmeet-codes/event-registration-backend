package com.registration.management.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionRequest {

    @NotBlank
    @Size(max = 50)
    private String permissionCode;

    @NotBlank
    @Size(max = 150)
    private String permissionName;

    @Size(max = 500)
    private String description;
}
