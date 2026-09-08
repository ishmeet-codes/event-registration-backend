package com.registration.management.auditManagement.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.registration.management.audit.entities.AuditLog;
import com.registration.management.auditManagement.dto.AuditLogResponseDTO;
import com.registration.management.auditManagement.dto.AuditSummaryDTO;
import com.registration.management.auditManagement.dto.AuditUserSummaryDTO;
import com.registration.management.auditManagement.enums.AuditModule;
import com.registration.management.auditManagement.repository.AuditManagementRepository;
import com.registration.management.auditManagement.service.AuditManagementService;
import com.registration.management.auditManagement.specification.AuditLogSpecification;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditManagementServiceImpl implements AuditManagementService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdat", "action", "module", "status", "entityname", "entitytype", "id"
    );

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwordhash", "secret", "token", "refreshtoken", "accesstoken",
            "jwt", "otp", "apikey", "authorization", "credentials"
    );

    private static final int MAX_EXPORT_RECORDS = 5000;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditManagementRepository auditRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    // ─── 1. List / Search Audit Logs ───────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> getAuditLogs(
            String search,
            Long userId,
            String module,
            String actionStr,
            String entityType,
            Long entityId,
            String statusStr,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size,
            String sort
    ) {
        validateDateRange(dateFrom, dateTo);
        Pageable pageable = createPageable(page, size, sort);

        AuditAction action = parseAuditAction(actionStr);
        AuditStatus status = parseAuditStatus(statusStr);

        LocalDateTime startDateTime = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime endDateTime = dateTo != null ? dateTo.atTime(23, 59, 59, 999999999) : null;

        Specification<AuditLog> spec = AuditLogSpecification.withFilters(
                search, userId, module, action, entityType, entityId, status, startDateTime, endDateTime, false
        );

        return auditRepository.findAll(spec, pageable).map(this::mapToDTO);
    }

    // ─── 2. Get Audit Log by ID ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponseDTO getAuditLogById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Audit log ID cannot be null");
        }
        AuditLog auditLog = auditRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Audit log not found with ID: " + id));
        return mapToDTO(auditLog);
    }

    // ─── 3. Audit Statistics / Summary ─────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuditSummaryDTO getAuditSummary(
            LocalDate dateFrom,
            LocalDate dateTo,
            String module,
            Long userId
    ) {
        validateDateRange(dateFrom, dateTo);

        LocalDateTime startDateTime = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime endDateTime = dateTo != null ? dateTo.atTime(23, 59, 59, 999999999) : null;

        Specification<AuditLog> spec = AuditLogSpecification.withFilters(
                null, userId, module, null, null, null, null, startDateTime, endDateTime, false
        );

        List<AuditLog> logs = auditRepository.findAll(spec);

        long totalActions = logs.size();
        long successfulActions = logs.stream().filter(l -> l.getStatus() == AuditStatus.SUCCESS).count();
        long failedActions = logs.stream().filter(l -> l.getStatus() == AuditStatus.FAILED || l.getStatus() == AuditStatus.FAILURE).count();

        long loginAttempts = logs.stream().filter(l ->
                l.getAction() == AuditAction.LOGIN ||
                        l.getAction() == AuditAction.LOGIN_SUCCESS ||
                        l.getAction() == AuditAction.LOGIN_FAILED
        ).count();

        long failedLogins = logs.stream().filter(l ->
                l.getAction() == AuditAction.LOGIN_FAILED ||
                        (l.getAction() == AuditAction.LOGIN && (l.getStatus() == AuditStatus.FAILED || l.getStatus() == AuditStatus.FAILURE))
        ).count();

        long accessDenied = logs.stream().filter(l ->
                l.getAction() == AuditAction.ACCESS_DENIED ||
                        l.getAction() == AuditAction.UNAUTHORIZED_ACCESS_ATTEMPT
        ).count();

        Map<String, Long> moduleBreakdown = logs.stream()
                .filter(l -> l.getModule() != null)
                .collect(Collectors.groupingBy(AuditLog::getModule, Collectors.counting()));

        Map<String, Long> actionBreakdown = logs.stream()
                .filter(l -> l.getAction() != null)
                .collect(Collectors.groupingBy(l -> l.getAction().name(), Collectors.counting()));

        return AuditSummaryDTO.builder()
                .totalActions(totalActions)
                .successfulActions(successfulActions)
                .failedActions(failedActions)
                .loginAttempts(loginAttempts)
                .failedLogins(failedLogins)
                .accessDenied(accessDenied)
                .moduleBreakdown(moduleBreakdown)
                .actionBreakdown(actionBreakdown)
                .build();
    }

    // ─── 4. Export Audit Logs ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAuditLogs(
            String format,
            String module,
            String actionStr,
            String statusStr,
            Long userId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        if (format == null || (!format.equalsIgnoreCase("CSV") && !format.equalsIgnoreCase("XLSX"))) {
            throw new IllegalArgumentException("Invalid export format: " + format + ". Allowed formats: [CSV, XLSX]");
        }

        validateDateRange(dateFrom, dateTo);

        AuditAction action = parseAuditAction(actionStr);
        AuditStatus status = parseAuditStatus(statusStr);

        LocalDateTime startDateTime = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime endDateTime = dateTo != null ? dateTo.atTime(23, 59, 59, 999999999) : null;

        Specification<AuditLog> spec = AuditLogSpecification.withFilters(
                null, userId, module, action, null, null, status, startDateTime, endDateTime, false
        );

        Pageable pageable = PageRequest.of(0, MAX_EXPORT_RECORDS, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AuditLog> records = auditRepository.findAll(spec, pageable).getContent();

        if (format.equalsIgnoreCase("CSV")) {
            return generateCsv(records);
        } else {
            return generateXlsx(records);
        }
    }

    // ─── 5. User Activity ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> getUserActivity(
            Long userId,
            String search,
            String module,
            String actionStr,
            String statusStr,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size,
            String sort
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return getAuditLogs(search, userId, module, actionStr, null, null, statusStr, dateFrom, dateTo, page, size, sort);
    }

    // ─── 6. Entity History ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> getEntityHistory(
            String entityType,
            Long entityId,
            int page,
            int size,
            String sort
    ) {
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity type cannot be null or empty");
        }
        if (entityId == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }
        return getAuditLogs(null, null, null, null, entityType, entityId, null, null, null, page, size, sort);
    }

    // ─── 7. Security Activity ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> getSecurityActivity(
            String search,
            Long userId,
            String statusStr,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size,
            String sort
    ) {
        validateDateRange(dateFrom, dateTo);
        Pageable pageable = createPageable(page, size, sort);

        AuditStatus status = parseAuditStatus(statusStr);

        LocalDateTime startDateTime = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime endDateTime = dateTo != null ? dateTo.atTime(23, 59, 59, 999999999) : null;

        Specification<AuditLog> spec = AuditLogSpecification.withFilters(
                search, userId, null, null, null, null, status, startDateTime, endDateTime, true
        );

        return auditRepository.findAll(spec, pageable).map(this::mapToDTO);
    }

    // ─── 8. Recording Audit Events (System Context Resolution) ──────────────────

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String actionStr,
            String moduleStr,
            String entityType,
            Long entityId,
            String description,
            Object oldValue,
            Object newValue
    ) {
        AuditAction action = parseAuditAction(actionStr);
        if (action == null) {
            action = AuditAction.CREATE;
        }
        recordInternal(action, moduleStr, entityType, entityId, description, AuditStatus.SUCCESS, null, oldValue, newValue);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            AuditAction action,
            AuditModule module,
            String entityType,
            Long entityId,
            String description,
            Object oldValue,
            Object newValue
    ) {
        String moduleStr = module != null ? module.name() : null;
        recordInternal(action, moduleStr, entityType, entityId, description, AuditStatus.SUCCESS, null, oldValue, newValue);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            String actionStr,
            String moduleStr,
            String entityType,
            Long entityId,
            String description,
            String errorMessage,
            Object oldValue,
            Object newValue
    ) {
        AuditAction action = parseAuditAction(actionStr);
        if (action == null) {
            action = AuditAction.CREATE;
        }
        recordInternal(action, moduleStr, entityType, entityId, description, AuditStatus.FAILED, errorMessage, oldValue, newValue);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            AuditAction action,
            AuditModule module,
            String entityType,
            Long entityId,
            String description,
            String errorMessage,
            Object oldValue,
            Object newValue
    ) {
        String moduleStr = module != null ? module.name() : null;
        recordInternal(action, moduleStr, entityType, entityId, description, AuditStatus.FAILED, errorMessage, oldValue, newValue);
    }

    private void recordInternal(
            AuditAction action,
            String module,
            String entityType,
            Long entityId,
            String description,
            AuditStatus status,
            String errorMessage,
            Object oldValue,
            Object newValue
    ) {
        try {
            User currentUser = resolveSecurityContextUser();
            HttpServletRequest request = resolveHttpServletRequest();

            String ipAddress = extractClientIp(request);
            String userAgent = extractUserAgent(request);
            String requestId = extractRequestId(request);

            String oldValJson = serializeSanitizedJson(oldValue);
            String newValJson = serializeSanitizedJson(newValue);

            AuditLog auditLog = AuditLog.builder()
                    .user(currentUser)
                    .action(action)
                    .module(module)
                    .entityName(entityType)
                    .entityId(entityId)
                    .description(description)
                    .status(status)
                    .errorMessage(errorMessage)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .requestId(requestId)
                    .oldValue(oldValJson)
                    .newValue(newValJson)
                    .build();

            auditRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Failed to persist audit log record: {}", ex.getMessage(), ex);
        }
    }

    // ─── Helpers: Security Context & HTTP Request ──────────────────────────────

    private User resolveSecurityContextUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(String.valueOf(auth.getPrincipal()))) {
            return null;
        }
        if (auth.getPrincipal() instanceof User user) {
            return user;
        }
        String email = auth.getName();
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    private HttpServletRequest resolveHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) return "System/Internal";
        String ua = request.getHeader("User-Agent");
        return ua != null ? (ua.length() > 255 ? ua.substring(0, 255) : ua) : "Unknown";
    }

    private String extractRequestId(HttpServletRequest request) {
        if (request == null) return UUID.randomUUID().toString();
        String reqId = request.getHeader("X-Request-ID");
        if (reqId == null || reqId.isBlank()) {
            reqId = request.getHeader("X-Correlation-ID");
        }
        return reqId != null && !reqId.isBlank() ? reqId : UUID.randomUUID().toString();
    }

    // ─── Helpers: Sensitive Data Sanitization & JSON Serialization ──────────────

    private String serializeSanitizedJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String str) {
            if (str.trim().startsWith("{") || str.trim().startsWith("[")) {
                try {
                    JsonNode node = objectMapper.readTree(str);
                    sanitizeJsonNode(node);
                    return objectMapper.writeValueAsString(node);
                } catch (Exception e) {
                    return str;
                }
            }
            return "\"" + str.replace("\"", "\\\"") + "\"";
        }
        try {
            JsonNode node = objectMapper.valueToTree(obj);
            sanitizeJsonNode(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return null;
        }
    }

    private void sanitizeJsonNode(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);

            for (String field : fieldNames) {
                if (SENSITIVE_KEYS.contains(field.toLowerCase().replaceAll("[^a-z]", ""))) {
                    objectNode.put(field, "******");
                } else {
                    sanitizeJsonNode(objectNode.get(field));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                sanitizeJsonNode(child);
            }
        }
    }

    // ─── Helpers: Validation, Sort & Pageable ───────────────────────────────────

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must be before or equal to dateTo");
        }
    }

    private Pageable createPageable(int page, int size, String sortParam) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 25;
        } else if (size > 100) {
            size = 100;
        }

        Sort sort = parseSort(sortParam);
        return PageRequest.of(page, size, sort);
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        String normalizedProperty = property.toLowerCase().replace("_", "");

        if (!ALLOWED_SORT_FIELDS.contains(normalizedProperty)) {
            throw new IllegalArgumentException("Invalid sort field: " + property + ". Allowed fields: [createdAt, action, module, status, entityType, entityName, id]");
        }

        // Map alias to entity property name
        String entityField = switch (normalizedProperty) {
            case "createdat" -> "createdAt";
            case "entitytype", "entityname" -> "entityName";
            case "action" -> "action";
            case "module" -> "module";
            case "status" -> "status";
            case "id" -> "id";
            default -> "createdAt";
        };

        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }

        return Sort.by(direction, entityField);
    }

    private AuditAction parseAuditAction(String actionStr) {
        if (actionStr == null || actionStr.trim().isEmpty()) return null;
        try {
            return AuditAction.valueOf(actionStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private AuditStatus parseAuditStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) return null;
        try {
            return AuditStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ─── Helpers: Mapping to Response DTO ──────────────────────────────────────

    private AuditLogResponseDTO mapToDTO(AuditLog log) {
        if (log == null) return null;

        AuditUserSummaryDTO userDTO = null;
        if (log.getUser() != null) {
            User u = log.getUser();
            userDTO = AuditUserSummaryDTO.builder()
                    .id(u.getId())
                    .name(u.getFullName())
                    .email(u.getEmail())
                    .role(u.getRole() != null ? u.getRole().getRoleCode() : null)
                    .build();
        }

        Object parsedOldValue = parseJsonString(log.getOldValue());
        Object parsedNewValue = parseJsonString(log.getNewValue());

        return AuditLogResponseDTO.builder()
                .id(log.getId())
                .user(userDTO)
                .action(log.getAction() != null ? log.getAction().name() : null)
                .module(log.getModule())
                .entityType(log.getEntityName())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .status(log.getStatus() != null ? log.getStatus().name() : null)
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .requestId(log.getRequestId())
                .oldValue(parsedOldValue)
                .newValue(parsedNewValue)
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private Object parseJsonString(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    // ─── Helpers: CSV / XLSX Generation ────────────────────────────────────────

    private byte[] generateCsv(List<AuditLog> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Timestamp,User ID,User Name,User Email,Action,Module,Entity Type,Entity ID,Status,Description,IP Address,Request ID\n");

        for (AuditLog log : records) {
            String id = String.valueOf(log.getId());
            String timestamp = log.getCreatedAt() != null ? log.getCreatedAt().format(DATE_TIME_FORMATTER) : "";
            String userId = log.getUser() != null && log.getUser().getId() != null ? String.valueOf(log.getUser().getId()) : "";
            String userName = log.getUser() != null && log.getUser().getFullName() != null ? escapeCsv(log.getUser().getFullName()) : "";
            String userEmail = log.getUser() != null && log.getUser().getEmail() != null ? escapeCsv(log.getUser().getEmail()) : "";
            String action = log.getAction() != null ? log.getAction().name() : "";
            String module = log.getModule() != null ? escapeCsv(log.getModule()) : "";
            String entityType = log.getEntityName() != null ? escapeCsv(log.getEntityName()) : "";
            String entityId = log.getEntityId() != null ? String.valueOf(log.getEntityId()) : "";
            String status = log.getStatus() != null ? log.getStatus().name() : "";
            String description = log.getDescription() != null ? escapeCsv(log.getDescription()) : "";
            String ipAddress = log.getIpAddress() != null ? escapeCsv(log.getIpAddress()) : "";
            String requestId = log.getRequestId() != null ? escapeCsv(log.getRequestId()) : "";

            sb.append(String.join(",",
                    id, timestamp, userId, userName, userEmail, action, module, entityType, entityId, status, description, ipAddress, requestId
            )).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private byte[] generateXlsx(List<AuditLog> records) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Audit Logs");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] headers = {
                    "ID", "Timestamp", "User ID", "User Name", "User Email", "Action",
                    "Module", "Entity Type", "Entity ID", "Status", "Description", "IP Address", "Request ID"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (AuditLog log : records) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(log.getId() != null ? log.getId() : 0);
                row.createCell(1).setCellValue(log.getCreatedAt() != null ? log.getCreatedAt().format(DATE_TIME_FORMATTER) : "");
                row.createCell(2).setCellValue(log.getUser() != null && log.getUser().getId() != null ? String.valueOf(log.getUser().getId()) : "");
                row.createCell(3).setCellValue(log.getUser() != null && log.getUser().getFullName() != null ? log.getUser().getFullName() : "");
                row.createCell(4).setCellValue(log.getUser() != null && log.getUser().getEmail() != null ? log.getUser().getEmail() : "");
                row.createCell(5).setCellValue(log.getAction() != null ? log.getAction().name() : "");
                row.createCell(6).setCellValue(log.getModule() != null ? log.getModule() : "");
                row.createCell(7).setCellValue(log.getEntityName() != null ? log.getEntityName() : "");
                row.createCell(8).setCellValue(log.getEntityId() != null ? String.valueOf(log.getEntityId()) : "");
                row.createCell(9).setCellValue(log.getStatus() != null ? log.getStatus().name() : "");
                row.createCell(10).setCellValue(log.getDescription() != null ? log.getDescription() : "");
                row.createCell(11).setCellValue(log.getIpAddress() != null ? log.getIpAddress() : "");
                row.createCell(12).setCellValue(log.getRequestId() != null ? log.getRequestId() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate XLSX spreadsheet for audit logs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export audit logs to Excel", e);
        }
    }
}
