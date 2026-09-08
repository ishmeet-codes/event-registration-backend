package com.registration.management.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.registration.management.common.exception.GlobalExceptionHandler;
import com.registration.management.notification.dto.*;
import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationStatus;
import com.registration.management.notification.enums.NotificationType;
import com.registration.management.notification.enums.RecipientTargetType;
import com.registration.management.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setMessageConverters(converter)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ─── 1. Create Notification ──────────────────────────────────────────────────

    @Test
    @DisplayName("1. POST /api/notifications creates a notification (201 Created)")
    void testCreateNotification() throws Exception {
        NotificationRequestDTO request = NotificationRequestDTO.builder()
                .recipientUserId(10L)
                .type(NotificationType.ANNOUNCEMENT)
                .channel(NotificationChannel.IN_APP)
                .title("New Update")
                .message("System updated")
                .build();

        NotificationResponseDTO response = NotificationResponseDTO.builder()
                .id(1L)
                .recipientUserId(10L)
                .type(NotificationType.ANNOUNCEMENT)
                .channel(NotificationChannel.IN_APP)
                .title("New Update")
                .message("System updated")
                .status(NotificationStatus.SENT)
                .build();

        when(notificationService.createNotification(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("New Update"));
    }

    // ─── 2. List Notifications (Inbox / Filter) ─────────────────────────────────

    @Test
    @DisplayName("2. GET /api/notifications returns paginated notifications (200 OK)")
    void testGetNotifications() throws Exception {
        NotificationResponseDTO item = NotificationResponseDTO.builder()
                .id(1L)
                .recipientUserId(10L)
                .type(NotificationType.ANNOUNCEMENT)
                .channel(NotificationChannel.IN_APP)
                .title("Welcome")
                .status(NotificationStatus.SENT)
                .build();

        when(notificationService.getNotifications(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/notifications")
                        .param("search", "Welcome")
                        .param("type", "ANNOUNCEMENT")
                        .param("channel", "IN_APP")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Welcome"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ─── 3. Notification Details ─────────────────────────────────────────────────

    @Test
    @DisplayName("3. GET /api/notifications/{id} returns notification by ID (200 OK)")
    void testGetNotificationById() throws Exception {
        NotificationResponseDTO response = NotificationResponseDTO.builder()
                .id(5L)
                .recipientUserId(10L)
                .title("Event Alert")
                .status(NotificationStatus.SENT)
                .build();

        when(notificationService.getNotificationById(eq(5L), any())).thenReturn(response);

        mockMvc.perform(get("/api/notifications/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.title").value("Event Alert"));
    }

    // ─── 4. Mark as Read ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("4. PATCH /api/notifications/{id}/read marks notification as read (200 OK)")
    void testMarkAsRead() throws Exception {
        NotificationResponseDTO response = NotificationResponseDTO.builder()
                .id(5L)
                .read(true)
                .readAt(LocalDateTime.now())
                .build();

        when(notificationService.markAsRead(eq(5L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/notifications/5/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.read").value(true));
    }

    // ─── 5. Mark All Read ────────────────────────────────────────────────────────

    @Test
    @DisplayName("5. PATCH /api/notifications/read-all marks all as read (200 OK)")
    void testMarkAllRead() throws Exception {
        when(notificationService.markAllAsRead(any()))
                .thenReturn(Map.of("updatedCount", 4, "message", "Marked 4 notifications as read"));

        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(4));
    }

    // ─── 6. Unread Count Badge ───────────────────────────────────────────────────

    @Test
    @DisplayName("6. GET /api/notifications/unread-count returns unread count (200 OK)")
    void testGetUnreadCount() throws Exception {
        when(notificationService.getUnreadCount(any())).thenReturn(UnreadCountDTO.builder().count(7L).build());

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7L));
    }

    // ─── 7. Delete Notification ──────────────────────────────────────────────────

    @Test
    @DisplayName("7. DELETE /api/notifications/{id} deletes notification (204 No Content)")
    void testDeleteNotification() throws Exception {
        doNothing().when(notificationService).deleteNotification(eq(10L), any());

        mockMvc.perform(delete("/api/notifications/10"))
                .andExpect(status().isNoContent());
    }

    // ─── 8. Bulk Notification ────────────────────────────────────────────────────

    @Test
    @DisplayName("8. POST /api/notifications/bulk sends bulk notifications (200 OK)")
    void testSendBulkNotification() throws Exception {
        BulkNotificationRequestDTO request = BulkNotificationRequestDTO.builder()
                .recipientUserIds(List.of(1L, 2L, 3L))
                .targetType(RecipientTargetType.ALL_USERS)
                .type(NotificationType.ANNOUNCEMENT)
                .channel(NotificationChannel.EMAIL)
                .title("Bulk Notice")
                .message("Test message")
                .build();

        BulkNotificationResponseDTO response = BulkNotificationResponseDTO.builder()
                .requestedCount(3)
                .queuedCount(3)
                .failedCount(0)
                .notificationIds(List.of(101L, 102L, 103L))
                .message("Queued 3 notifications")
                .build();

        when(notificationService.sendBulkNotification(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/notifications/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(3))
                .andExpect(jsonPath("$.queuedCount").value(3));
    }

    // ─── 9. Schedule Notification ────────────────────────────────────────────────

    @Test
    @DisplayName("9. POST /api/notifications/schedule schedules notification (202 Accepted)")
    void testScheduleNotification() throws Exception {
        ScheduleNotificationRequestDTO request = ScheduleNotificationRequestDTO.builder()
                .recipientUserIds(List.of(1L))
                .targetType(RecipientTargetType.ALL_USERS)
                .type(NotificationType.EVENT_REMINDER)
                .channel(NotificationChannel.IN_APP)
                .title("Reminder")
                .message("Event starting soon")
                .scheduledAt(LocalDateTime.now().plusDays(2))
                .build();

        BulkNotificationResponseDTO response = BulkNotificationResponseDTO.builder()
                .requestedCount(1)
                .queuedCount(1)
                .failedCount(0)
                .notificationIds(List.of(201L))
                .message("Scheduled 1 notification")
                .build();

        when(notificationService.scheduleNotification(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/notifications/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestedCount").value(1))
                .andExpect(jsonPath("$.queuedCount").value(1));
    }

    // ─── 10. Cancel Scheduled Notification ──────────────────────────────────────

    @Test
    @DisplayName("10. DELETE /api/notifications/scheduled/{id} cancels scheduled notification (204 No Content)")
    void testCancelScheduledNotification() throws Exception {
        doNothing().when(notificationService).cancelScheduledNotification(eq(25L), any());

        mockMvc.perform(delete("/api/notifications/scheduled/25"))
                .andExpect(status().isNoContent());
    }

    // ─── 11. Scheduled Notifications List ───────────────────────────────────────

    @Test
    @DisplayName("11. GET /api/notifications/scheduled returns scheduled notifications (200 OK)")
    void testGetScheduledNotifications() throws Exception {
        NotificationResponseDTO item = NotificationResponseDTO.builder()
                .id(201L)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.PENDING)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .build();

        when(notificationService.getScheduledNotifications(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/notifications/scheduled")
                        .param("channel", "IN_APP")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(201L))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ─── 12. Delivery Details ───────────────────────────────────────────────────

    @Test
    @DisplayName("12. GET /api/notifications/delivery/{id} returns delivery details (200 OK)")
    void testGetDeliveryDetails() throws Exception {
        DeliveryDetailsDTO response = DeliveryDetailsDTO.builder()
                .notificationId(50L)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.DELIVERED)
                .attempts(1)
                .recipientEmail("test@example.com")
                .build();

        when(notificationService.getDeliveryDetails(eq(50L), any())).thenReturn(response);

        mockMvc.perform(get("/api/notifications/delivery/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(50L))
                .andExpect(jsonPath("$.recipientEmail").value("test@example.com"))
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    // ─── 13. Retry Failed Notification ──────────────────────────────────────────

    @Test
    @DisplayName("13. POST /api/notifications/{id}/retry retries failed notification (200 OK)")
    void testRetryNotification() throws Exception {
        NotificationResponseDTO response = NotificationResponseDTO.builder()
                .id(60L)
                .status(NotificationStatus.SENT)
                .retryCount(1)
                .build();

        when(notificationService.retryNotification(eq(60L), any())).thenReturn(response);

        mockMvc.perform(post("/api/notifications/60/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(60L))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.retryCount").value(1));
    }

    // ─── 14. List Templates ─────────────────────────────────────────────────────

    @Test
    @DisplayName("14. GET /api/notifications/templates returns template page (200 OK)")
    void testGetTemplates() throws Exception {
        NotificationTemplateResponseDTO template = NotificationTemplateResponseDTO.builder()
                .id(1L)
                .code("EVENT_REMINDER_EMAIL")
                .name("Event Reminder")
                .type(NotificationType.EVENT_REMINDER)
                .channel(NotificationChannel.EMAIL)
                .active(true)
                .build();

        when(notificationService.getTemplates(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(template), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/notifications/templates")
                        .param("type", "EVENT_REMINDER")
                        .param("channel", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("EVENT_REMINDER_EMAIL"))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ─── 15. Create Template ────────────────────────────────────────────────────

    @Test
    @DisplayName("15. POST /api/notifications/templates creates template (201 Created)")
    void testCreateTemplate() throws Exception {
        NotificationTemplateRequestDTO request = NotificationTemplateRequestDTO.builder()
                .code("REG_SUBMIT")
                .name("Registration Submitted")
                .type(NotificationType.REGISTRATION_SUBMITTED)
                .channel(NotificationChannel.EMAIL)
                .subject("Registration Received")
                .body("Hello {{recipientName}}, your registration is received.")
                .active(true)
                .build();

        NotificationTemplateResponseDTO response = NotificationTemplateResponseDTO.builder()
                .id(2L)
                .code("REG_SUBMIT")
                .name("Registration Submitted")
                .type(NotificationType.REGISTRATION_SUBMITTED)
                .channel(NotificationChannel.EMAIL)
                .subject("Registration Received")
                .body("Hello {{recipientName}}, your registration is received.")
                .active(true)
                .build();

        when(notificationService.createTemplate(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/notifications/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.code").value("REG_SUBMIT"));
    }

    // ─── 16. Template Details ───────────────────────────────────────────────────

    @Test
    @DisplayName("16. GET /api/notifications/templates/{id} returns template by ID (200 OK)")
    void testGetTemplateById() throws Exception {
        NotificationTemplateResponseDTO response = NotificationTemplateResponseDTO.builder()
                .id(2L)
                .code("REG_SUBMIT")
                .name("Registration Submitted")
                .build();

        when(notificationService.getTemplateById(eq(2L))).thenReturn(response);

        mockMvc.perform(get("/api/notifications/templates/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.code").value("REG_SUBMIT"));
    }

    // ─── 17. Update Template ────────────────────────────────────────────────────

    @Test
    @DisplayName("17. PUT /api/notifications/templates/{id} updates template (200 OK)")
    void testUpdateTemplate() throws Exception {
        NotificationTemplateRequestDTO request = NotificationTemplateRequestDTO.builder()
                .code("REG_SUBMIT")
                .name("Registration Submitted Updated")
                .type(NotificationType.REGISTRATION_SUBMITTED)
                .channel(NotificationChannel.EMAIL)
                .subject("Registration Received - Updated")
                .body("Hello {{recipientName}}, updated body.")
                .active(true)
                .build();

        NotificationTemplateResponseDTO response = NotificationTemplateResponseDTO.builder()
                .id(2L)
                .code("REG_SUBMIT")
                .name("Registration Submitted Updated")
                .type(NotificationType.REGISTRATION_SUBMITTED)
                .channel(NotificationChannel.EMAIL)
                .active(true)
                .build();

        when(notificationService.updateTemplate(eq(2L), any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/notifications/templates/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Registration Submitted Updated"));
    }

    // ─── 18. Delete Template ────────────────────────────────────────────────────

    @Test
    @DisplayName("18. DELETE /api/notifications/templates/{id} deletes template (204 No Content)")
    void testDeleteTemplate() throws Exception {
        doNothing().when(notificationService).deleteTemplate(eq(2L), any());

        mockMvc.perform(delete("/api/notifications/templates/2"))
                .andExpect(status().isNoContent());
    }

    // ─── 19. Update Template Status ─────────────────────────────────────────────

    @Test
    @DisplayName("19. PATCH /api/notifications/templates/{id}/status updates template active state (200 OK)")
    void testUpdateTemplateStatus() throws Exception {
        TemplateStatusUpdateRequestDTO request = TemplateStatusUpdateRequestDTO.builder()
                .active(false)
                .build();

        NotificationTemplateResponseDTO response = NotificationTemplateResponseDTO.builder()
                .id(2L)
                .code("REG_SUBMIT")
                .active(false)
                .build();

        when(notificationService.updateTemplateStatus(eq(2L), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/notifications/templates/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.active").value(false));
    }

    // ─── 20. Get Own Preferences ────────────────────────────────────────────────

    @Test
    @DisplayName("20. GET /api/notifications/preferences returns user preferences (200 OK)")
    void testGetPreferences() throws Exception {
        NotificationPreferenceDTO pref = NotificationPreferenceDTO.builder()
                .userId(5L)
                .emailEnabled(true)
                .inAppEnabled(true)
                .smsEnabled(false)
                .build();

        when(notificationService.getPreferences(any())).thenReturn(pref);

        mockMvc.perform(get("/api/notifications/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.smsEnabled").value(false));
    }

    // ─── 21. Update Own Preferences ─────────────────────────────────────────────

    @Test
    @DisplayName("21. PUT /api/notifications/preferences updates preferences (200 OK)")
    void testUpdatePreferences() throws Exception {
        NotificationPreferenceDTO request = NotificationPreferenceDTO.builder()
                .userId(5L)
                .emailEnabled(false)
                .inAppEnabled(true)
                .smsEnabled(true)
                .build();

        NotificationPreferenceDTO response = NotificationPreferenceDTO.builder()
                .userId(5L)
                .emailEnabled(false)
                .inAppEnabled(true)
                .smsEnabled(true)
                .build();

        when(notificationService.updatePreferences(any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.smsEnabled").value(true));
    }

    // ─── 22. Notification Summary / Analytics ───────────────────────────────────

    @Test
    @DisplayName("22. GET /api/notifications/summary returns summary analytics (200 OK)")
    void testGetSummary() throws Exception {
        NotificationSummaryDTO summary = NotificationSummaryDTO.builder()
                .total(100)
                .sent(90)
                .delivered(85)
                .failed(5)
                .read(60)
                .pending(5)
                .scheduled(3)
                .cancelled(2)
                .build();

        when(notificationService.getSummary(any(), any(), any(), any())).thenReturn(summary);

        mockMvc.perform(get("/api/notifications/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.sent").value(90))
                .andExpect(jsonPath("$.failed").value(5));
    }

    // ─── 23. Delivery / Notification Logs ───────────────────────────────────────

    @Test
    @DisplayName("23. GET /api/notifications/logs returns notification logs (200 OK)")
    void testGetLogs() throws Exception {
        NotificationLogDTO log = NotificationLogDTO.builder()
                .id(1L)
                .notificationId(10L)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.DELIVERED)
                .recipientEmail("user@example.com")
                .attempts(1)
                .build();

        when(notificationService.getLogs(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/notifications/logs")
                        .param("status", "DELIVERED")
                        .param("channel", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].recipientEmail").value("user@example.com"))
                .andExpect(jsonPath("$.content[0].status").value("DELIVERED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
