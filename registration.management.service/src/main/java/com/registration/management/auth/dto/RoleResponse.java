package com.registration.management.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RoleResponse {
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Boolean systemRole;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PermissionResponse> permissions;
}
