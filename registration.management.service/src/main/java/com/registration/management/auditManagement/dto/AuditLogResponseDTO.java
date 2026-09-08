package com.registration.management.auditManagement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponseDTO {
    private Long id;
    private AuditUserSummaryDTO user;
    private String action;
    private String module;
    private String entityType;
    private Long entityId;
    private String description;
    private String status;
    private String ipAddress;
    private String userAgent;
    private String requestId;
    private Object oldValue;
    private Object newValue;
    private String errorMessage;
    private LocalDateTime createdAt;
}
