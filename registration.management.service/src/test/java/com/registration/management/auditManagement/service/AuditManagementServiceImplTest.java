package com.registration.management.auditManagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.registration.management.audit.entities.AuditLog;
import com.registration.management.auditManagement.dto.AuditLogResponseDTO;
import com.registration.management.auditManagement.dto.AuditSummaryDTO;
import com.registration.management.auditManagement.enums.AuditModule;
import com.registration.management.auditManagement.repository.AuditManagementRepository;
import com.registration.management.auditManagement.serviceImpl.AuditManagementServiceImpl;
import com.registration.management.auth.entities.Role;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AuditManagementServiceImplTest {

    @Mock
    private AuditManagementRepository auditRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private AuditManagementServiceImpl auditService;

    private User sampleUser;
    private AuditLog sampleLog;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().id(1L).roleCode("ADMIN").roleName("Admin").build();
        sampleUser = User.builder()
                .id(10L)
                .email("admin@test.com")
                .fullName("Admin User")
                .role(role)
                .build();

        sampleLog = AuditLog.builder()
                .id(1L)
                .user(sampleUser)
                .action(AuditAction.REGISTRATION_APPROVED)
                .module("REGISTRATION")
                .entityName("REGISTRATION")
                .entityId(101L)
                .description("Registration approved")
                .status(AuditStatus.SUCCESS)
                .ipAddress("127.0.0.1")
                .userAgent("TestAgent")
                .requestId("req-abc")
                .oldValue("{\"status\":\"SUBMITTED\"}")
                .newValue("{\"status\":\"APPROVED\"}")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── 1. Get Audit Logs ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getAuditLogs with filters returns mapped page")
    void testGetAuditLogs() {
        when(auditRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        Page<AuditLogResponseDTO> result = auditService.getAuditLogs(
                "REGISTRATION", 10L, "REGISTRATION", "REGISTRATION_APPROVED",
                "REGISTRATION", 101L, "SUCCESS",
                LocalDate.now().minusDays(1), LocalDate.now(),
                0, 25, "createdAt,desc"
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        AuditLogResponseDTO dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getAction()).isEqualTo("REGISTRATION_APPROVED");
        assertThat(dto.getUser().getEmail()).isEqualTo("admin@test.com");
    }

    @Test
    @DisplayName("getAuditLogs with invalid sort field throws IllegalArgumentException")
    void testGetAuditLogs_InvalidSortField() {
        assertThatThrownBy(() -> auditService.getAuditLogs(
                null, null, null, null, null, null, null,
                null, null, 0, 25, "password,desc"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field");
    }

    @Test
    @DisplayName("getAuditLogs with invalid date range throws IllegalArgumentException")
    void testGetAuditLogs_InvalidDateRange() {
        assertThatThrownBy(() -> auditService.getAuditLogs(
                null, null, null, null, null, null, null,
                LocalDate.now().plusDays(2), LocalDate.now(), 0, 25, "createdAt,desc"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFrom must be before or equal to dateTo");
    }

    // ─── 2. Get Audit Log by ID ─────────────────────────────────────────────────

    @Test
    @DisplayName("getAuditLogById returns DTO when found")
    void testGetAuditLogById_Success() {
        when(auditRepository.findById(1L)).thenReturn(Optional.of(sampleLog));

        AuditLogResponseDTO dto = auditService.getAuditLogById(1L);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getEntityId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("getAuditLogById throws NoSuchElementException when not found")
    void testGetAuditLogById_NotFound() {
        when(auditRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getAuditLogById(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ─── 3. Get Audit Summary ───────────────────────────────────────────────────

    @Test
    @DisplayName("getAuditSummary calculates aggregate statistics accurately")
    void testGetAuditSummary() {
        AuditLog loginFailed = AuditLog.builder()
                .id(2L)
                .action(AuditAction.LOGIN_FAILED)
                .module("AUTH")
                .status(AuditStatus.FAILED)
                .createdAt(LocalDateTime.now())
                .build();

        AuditLog accessDenied = AuditLog.builder()
                .id(3L)
                .action(AuditAction.ACCESS_DENIED)
                .module("SECURITY")
                .status(AuditStatus.FAILED)
                .createdAt(LocalDateTime.now())
                .build();

        when(auditRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(sampleLog, loginFailed, accessDenied));

        AuditSummaryDTO summary = auditService.getAuditSummary(null, null, null, null);

        assertThat(summary.getTotalActions()).isEqualTo(3);
        assertThat(summary.getSuccessfulActions()).isEqualTo(1);
        assertThat(summary.getFailedActions()).isEqualTo(2);
        assertThat(summary.getLoginAttempts()).isEqualTo(1);
        assertThat(summary.getFailedLogins()).isEqualTo(1);
        assertThat(summary.getAccessDenied()).isEqualTo(1);
        assertThat(summary.getModuleBreakdown()).containsEntry("REGISTRATION", 1L);
        assertThat(summary.getModuleBreakdown()).containsEntry("AUTH", 1L);
    }

    // ─── 4. Export Audit Logs (CSV & XLSX) ───────────────────────────────────────

    @Test
    @DisplayName("exportAuditLogs CSV format produces valid CSV bytes")
    void testExportAuditLogs_Csv() {
        when(auditRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        byte[] csvBytes = auditService.exportAuditLogs("CSV", null, null, null, null, null, null);

        assertThat(csvBytes).isNotEmpty();
        String csvString = new String(csvBytes, StandardCharsets.UTF_8);
        assertThat(csvString).contains("ID,Timestamp,User ID,User Name,User Email,Action,Module,Entity Type,Entity ID,Status,Description,IP Address,Request ID");
        assertThat(csvString).contains("REGISTRATION_APPROVED");
        assertThat(csvString).contains("admin@test.com");
    }

    @Test
    @DisplayName("exportAuditLogs XLSX format produces valid Excel workbook")
    void testExportAuditLogs_Xlsx() throws Exception {
        when(auditRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        byte[] xlsxBytes = auditService.exportAuditLogs("XLSX", null, null, null, null, null, null);

        assertThat(xlsxBytes).isNotEmpty();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            Sheet sheet = workbook.getSheet("Audit Logs");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2); // Header + 1 row
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("ID");
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).isEqualTo("REGISTRATION_APPROVED");
        }
    }

    @Test
    @DisplayName("exportAuditLogs with invalid format throws IllegalArgumentException")
    void testExportAuditLogs_InvalidFormat() {
        assertThatThrownBy(() -> auditService.exportAuditLogs("XML", null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid export format");
    }

    // ─── 5. User Activity & Entity History ──────────────────────────────────────

    @Test
    @DisplayName("getUserActivity retrieves records for specific userId")
    void testGetUserActivity() {
        when(auditRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        Page<AuditLogResponseDTO> result = auditService.getUserActivity(10L, null, null, null, null, null, null, 0, 25, "createdAt,desc");
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getEntityHistory retrieves audit records for specific entity")
    void testGetEntityHistory() {
        when(auditRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLog)));

        Page<AuditLogResponseDTO> result = auditService.getEntityHistory("REGISTRATION", 101L, 0, 25, "createdAt,desc");
        assertThat(result.getContent()).hasSize(1);
    }

    // ─── 6. Record & RecordFailure with Sensitive Data Sanitization ─────────────

    @Test
    @DisplayName("record successfully saves AuditLog and masks sensitive data")
    void testRecord_MasksSensitiveData() {
        Map<String, Object> secretPayload = Map.of(
                "username", "john_doe",
                "password", "superSecret123",
                "apiKey", "xyz-12345"
        );

        auditService.record(
                AuditAction.LOGIN_SUCCESS,
                AuditModule.AUTH,
                "USER",
                10L,
                "User login",
                null,
                secretPayload
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.LOGIN_SUCCESS);
        assertThat(saved.getModule()).isEqualTo("AUTH");
        assertThat(saved.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(saved.getNewValue()).contains("******");
        assertThat(saved.getNewValue()).doesNotContain("superSecret123");
        assertThat(saved.getNewValue()).doesNotContain("xyz-12345");
        assertThat(saved.getNewValue()).contains("john_doe");
    }

    @Test
    @DisplayName("recordFailure records failure audit log with error message")
    void testRecordFailure() {
        auditService.recordFailure(
                AuditAction.ACCESS_DENIED,
                AuditModule.SECURITY,
                "EVENT",
                50L,
                "Unauthorized delete attempt",
                "User has no permission EVENT_DELETE",
                null,
                null
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(saved.getStatus()).isEqualTo(AuditStatus.FAILED);
        assertThat(saved.getErrorMessage()).isEqualTo("User has no permission EVENT_DELETE");
    }
}
