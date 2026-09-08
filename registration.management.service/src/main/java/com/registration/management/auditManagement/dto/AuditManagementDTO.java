package com.registration.management.auditManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditManagementDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String action;
    private String module;
    private String entityType;
    private Long entityId;
    private String description;
    private String status;
    private String ipAddress;
    private String userAgent;
    private String requestId;
    private LocalDateTime createdAt;
}
