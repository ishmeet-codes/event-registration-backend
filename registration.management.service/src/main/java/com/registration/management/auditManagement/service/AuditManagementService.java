package com.registration.management.auditManagement.service;

import com.registration.management.auditManagement.dto.AuditLogResponseDTO;
import com.registration.management.auditManagement.dto.AuditSummaryDTO;
import com.registration.management.auditManagement.enums.AuditModule;
import com.registration.management.enums.AuditAction;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface AuditManagementService {

    Page<AuditLogResponseDTO> getAuditLogs(
            String search,
            Long userId,
            String module,
            String action,
            String entityType,
            Long entityId,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size,
            String sort
    );

    AuditLogResponseDTO getAuditLogById(Long id);

    AuditSummaryDTO getAuditSummary(
            LocalDate dateFrom,
            LocalDate dateTo,
            String module,
            Long userId
    );

    byte[] exportAuditLogs(
            String format,
            String module,
            String action,
            String status,
            Long userId,
            LocalDate dateFrom,
            LocalDate dateTo
    );

    Page<AuditLogResponseDTO> getUserActivity(
            Long userId,
            String search,
            String module,
            String action,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size,
            String sort
    );

    Page<AuditLogResponseDTO> getEntityHistory(
            String entityType,
            Long entityId,
            int page,
            int size,
            String sort
    );

    Page<AuditLogResponseDTO> getSecurityActivity(
            String search,
            Long userId,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size,
            String sort
    );

    void record(
            String action,
            String module,
            String entityType,
            Long entityId,
            String description,
            Object oldValue,
            Object newValue
    );

    void record(
            AuditAction action,
            AuditModule module,
            String entityType,
            Long entityId,
            String description,
            Object oldValue,
            Object newValue
    );

    void recordFailure(
            String action,
            String module,
            String entityType,
            Long entityId,
            String description,
            String errorMessage,
            Object oldValue,
            Object newValue
    );

    void recordFailure(
            AuditAction action,
            AuditModule module,
            String entityType,
            Long entityId,
            String description,
            String errorMessage,
            Object oldValue,
            Object newValue
    );
}
