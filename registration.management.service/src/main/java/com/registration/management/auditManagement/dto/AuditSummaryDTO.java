package com.registration.management.auditManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSummaryDTO {
    private long totalActions;
    private long successfulActions;
    private long failedActions;
    private long loginAttempts;
    private long failedLogins;
    private long accessDenied;
    private Map<String, Long> moduleBreakdown;
    private Map<String, Long> actionBreakdown;
}
