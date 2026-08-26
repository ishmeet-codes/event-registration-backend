package com.registration.management.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionsRequest {

    @NotEmpty
    private List<Long> permissionIds;
}
