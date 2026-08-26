package com.registration.management.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PermissionResponse {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String description;
    private LocalDateTime createdAt;
}
