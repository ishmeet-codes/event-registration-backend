package com.registration.management.auditManagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.registration.management.auditManagement.dto.AuditLogResponseDTO;
import com.registration.management.auditManagement.dto.AuditSummaryDTO;
import com.registration.management.auditManagement.dto.AuditUserSummaryDTO;
import com.registration.management.auditManagement.service.AuditManagementService;
import com.registration.management.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuditManagementControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuditManagementService auditManagementService;

    @InjectMocks
    private AuditManagementController auditManagementController;

    private AuditLogResponseDTO sampleAuditLog;
    private AuditSummaryDTO sampleSummary;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(auditManagementController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleAuditLog = AuditLogResponseDTO.builder()
                .id(100L)
                .user(AuditUserSummaryDTO.builder()
                        .id(1L)
                        .name("Admin User")
                        .email("admin@test.com")
                        .role("ADMIN")
                        .build())
                .action("REGISTRATION_APPROVED")
                .module("REGISTRATION")
                .entityType("REGISTRATION")
                .entityId(55L)
                .description("Registration approved by admin")
                .status("SUCCESS")
                .ipAddress("192.168.1.10")
                .userAgent("Mozilla/5.0")
                .requestId("req-12345")
                .oldValue(Map.of("status", "SUBMITTED"))
                .newValue(Map.of("status", "APPROVED"))
                .createdAt(LocalDateTime.now())
                .build();

        sampleSummary = AuditSummaryDTO.builder()
                .totalActions(1500)
                .successfulActions(1450)
                .failedActions(50)
                .loginAttempts(300)
                .failedLogins(15)
                .accessDenied(8)
                .moduleBreakdown(Map.of("REGISTRATION", 500L, "AUTH", 300L))
                .actionBreakdown(Map.of("REGISTRATION_APPROVED", 200L, "LOGIN_SUCCESS", 285L))
                .build();
    }

    // ─── 1. GET /api/audit-logs (List & Search) ──────────────────────────────────

    @Test
    @DisplayName("1. GET /api/audit-logs returns 200 OK with paginated list")
    void testGetAuditLogs() throws Exception {
        when(auditManagementService.getAuditLogs(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyString()
        )).thenReturn(new PageImpl<>(List.of(sampleAuditLog), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/api/audit-logs")
                        .param("search", "REGISTRATION_APPROVED")
                        .param("module", "REGISTRATION")
                        .param("page", "0")
                        .param("size", "25")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].action").value("REGISTRATION_APPROVED"))
                .andExpect(jsonPath("$.content[0].module").value("REGISTRATION"))
                .andExpect(jsonPath("$.content[0].user.name").value("Admin User"));
    }

    @Test
    @DisplayName("1b. GET /api/audit-logs with invalid date range returns 400 Bad Request")
    void testGetAuditLogs_InvalidDateRange() throws Exception {
        when(auditManagementService.getAuditLogs(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyString()
        )).thenThrow(new IllegalArgumentException("dateFrom must be before or equal to dateTo"));

        mockMvc.perform(get("/api/audit-logs")
                        .param("dateFrom", "2026-12-01")
                        .param("dateTo", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("dateFrom must be before or equal to dateTo"));
    }

    // ─── 2. GET /api/audit-logs/{id} (Details) ───────────────────────────────────

    @Test
    @DisplayName("2. GET /api/audit-logs/{id} returns 200 OK with details")
    void testGetAuditLogById_Success() throws Exception {
        when(auditManagementService.getAuditLogById(100L)).thenReturn(sampleAuditLog);

        mockMvc.perform(get("/api/audit-logs/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.action").value("REGISTRATION_APPROVED"))
                .andExpect(jsonPath("$.entityId").value(55))
                .andExpect(jsonPath("$.ipAddress").value("192.168.1.10"));
    }

    @Test
    @DisplayName("2b. GET /api/audit-logs/{id} returns 404 when not found")
    void testGetAuditLogById_NotFound() throws Exception {
        when(auditManagementService.getAuditLogById(999L))
                .thenThrow(new NoSuchElementException("Audit log not found with ID: 999"));

        mockMvc.perform(get("/api/audit-logs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Audit log not found with ID: 999"));
    }

    // ─── 3. GET /api/audit-logs/summary (Statistics) ─────────────────────────────

    @Test
    @DisplayName("3. GET /api/audit-logs/summary returns 200 OK with statistics")
    void testGetAuditSummary() throws Exception {
        when(auditManagementService.getAuditSummary(any(), any(), any(), any()))
                .thenReturn(sampleSummary);

        mockMvc.perform(get("/api/audit-logs/summary")
                        .param("module", "REGISTRATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActions").value(1500))
                .andExpect(jsonPath("$.successfulActions").value(1450))
                .andExpect(jsonPath("$.failedActions").value(50))
                .andExpect(jsonPath("$.loginAttempts").value(300))
                .andExpect(jsonPath("$.failedLogins").value(15))
                .andExpect(jsonPath("$.accessDenied").value(8));
    }

    // ─── 4. GET /api/audit-logs/export (Export CSV/XLSX) ─────────────────────────

    @Test
    @DisplayName("4. GET /api/audit-logs/export?format=CSV returns 200 OK with CSV attachment")
    void testExportAuditLogs_Csv() throws Exception {
        byte[] csvBytes = "ID,Timestamp,User ID,Action\n100,2026-09-09,1,REGISTRATION_APPROVED".getBytes(StandardCharsets.UTF_8);
        when(auditManagementService.exportAuditLogs(eq("CSV"), any(), any(), any(), any(), any(), any()))
                .thenReturn(csvBytes);

        mockMvc.perform(get("/api/audit-logs/export")
                        .param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment; filename=\"audit_logs_")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString(".csv\"")))
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().bytes(csvBytes));
    }

    @Test
    @DisplayName("4b. GET /api/audit-logs/export?format=XLSX returns 200 OK with XLSX attachment")
    void testExportAuditLogs_Xlsx() throws Exception {
        byte[] xlsxBytes = new byte[]{1, 2, 3, 4};
        when(auditManagementService.exportAuditLogs(eq("XLSX"), any(), any(), any(), any(), any(), any()))
                .thenReturn(xlsxBytes);

        mockMvc.perform(get("/api/audit-logs/export")
                        .param("format", "XLSX"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString(".xlsx\"")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(xlsxBytes));
    }

    @Test
    @DisplayName("4c. GET /api/audit-logs/export with invalid format returns 400 Bad Request")
    void testExportAuditLogs_InvalidFormat() throws Exception {
        when(auditManagementService.exportAuditLogs(eq("PDF"), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid export format: PDF. Allowed formats: [CSV, XLSX]"));

        mockMvc.perform(get("/api/audit-logs/export")
                        .param("format", "PDF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid export format: PDF. Allowed formats: [CSV, XLSX]"));
    }

    // ─── 5. GET /api/audit-logs/security (Security Activity) ─────────────────────

    @Test
    @DisplayName("5. GET /api/audit-logs/security returns 200 OK with security activity")
    void testGetSecurityActivity() throws Exception {
        AuditLogResponseDTO securityLog = AuditLogResponseDTO.builder()
                .id(101L)
                .action("LOGIN_SUCCESS")
                .module("AUTH")
                .status("SUCCESS")
                .ipAddress("10.0.0.1")
                .createdAt(LocalDateTime.now())
                .build();

        when(auditManagementService.getSecurityActivity(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyString()))
                .thenReturn(new PageImpl<>(List.of(securityLog), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/api/audit-logs/security")
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.content[0].module").value("AUTH"));
    }

    // ─── 6. GET /api/audit-logs/user/{userId} (User Activity) ────────────────────

    @Test
    @DisplayName("6. GET /api/audit-logs/user/{userId} returns 200 OK with user's activity")
    void testGetUserActivity() throws Exception {
        when(auditManagementService.getUserActivity(eq(25L), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyString()))
                .thenReturn(new PageImpl<>(List.of(sampleAuditLog), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/api/audit-logs/user/25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100));
    }

    // ─── 7. GET /api/audit-logs/entity/{entityType}/{entityId} (Entity History) ──

    @Test
    @DisplayName("7. GET /api/audit-logs/entity/{entityType}/{entityId} returns 200 OK with entity history")
    void testGetEntityHistory() throws Exception {
        when(auditManagementService.getEntityHistory(eq("REGISTRATION"), eq(125L), anyInt(), anyInt(), anyString()))
                .thenReturn(new PageImpl<>(List.of(sampleAuditLog), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/api/audit-logs/entity/REGISTRATION/125"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("REGISTRATION"))
                .andExpect(jsonPath("$.content[0].entityId").value(55));
    }
}
