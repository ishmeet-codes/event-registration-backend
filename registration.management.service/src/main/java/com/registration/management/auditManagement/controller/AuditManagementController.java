package com.registration.management.auditManagement.controller;

import com.registration.management.auditManagement.dto.AuditLogResponseDTO;
import com.registration.management.auditManagement.dto.AuditSummaryDTO;
import com.registration.management.auditManagement.service.AuditManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditManagementController {

    private final AuditManagementService auditManagementService;

    // ─── 1. List / Search Audit Logs ───────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponseDTO>> getAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Page<AuditLogResponseDTO> result = auditManagementService.getAuditLogs(
                search, userId, module, action, entityType, entityId, status, dateFrom, dateTo, page, size, sort
        );
        return ResponseEntity.ok(result);
    }

    // ─── 2. Audit Statistics / Summary ─────────────────────────────────────────

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('AUDIT_LOG_ANALYTICS') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<AuditSummaryDTO> getAuditSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long userId
    ) {
        AuditSummaryDTO summary = auditManagementService.getAuditSummary(dateFrom, dateTo, module, userId);
        return ResponseEntity.ok(summary);
    }

    // ─── 3. Export Audit Logs ──────────────────────────────────────────────────

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('AUDIT_LOG_EXPORT') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(defaultValue = "CSV") String format,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        byte[] data = auditManagementService.exportAuditLogs(format, module, action, status, userId, dateFrom, dateTo);

        String fileExtension = format.equalsIgnoreCase("XLSX") ? "xlsx" : "csv";
        String mediaType = format.equalsIgnoreCase("XLSX")
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "text/csv";
        String filename = String.format("audit_logs_%s.%s", LocalDate.now().toString(), fileExtension);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(mediaType))
                .body(data);
    }

    // ─── 4. Security Activity ──────────────────────────────────────────────────

    @GetMapping("/security")
    @PreAuthorize("hasAuthority('SECURITY_ACTIVITY_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponseDTO>> getSecurityActivity(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Page<AuditLogResponseDTO> securityLogs = auditManagementService.getSecurityActivity(
                search, userId, status, dateFrom, dateTo, page, size, sort
        );
        return ResponseEntity.ok(securityLogs);
    }

    // ─── 5. User Activity ──────────────────────────────────────────────────────

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponseDTO>> getUserActivity(
            @PathVariable Long userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Page<AuditLogResponseDTO> userLogs = auditManagementService.getUserActivity(
                userId, search, module, action, status, dateFrom, dateTo, page, size, sort
        );
        return ResponseEntity.ok(userLogs);
    }

    // ─── 6. Entity History ─────────────────────────────────────────────────────

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponseDTO>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Page<AuditLogResponseDTO> entityLogs = auditManagementService.getEntityHistory(
                entityType, entityId, page, size, sort
        );
        return ResponseEntity.ok(entityLogs);
    }

    // ─── 7. Get Audit Log by ID ────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<AuditLogResponseDTO> getAuditLogById(@PathVariable Long id) {
        AuditLogResponseDTO dto = auditManagementService.getAuditLogById(id);
        return ResponseEntity.ok(dto);
    }
}
