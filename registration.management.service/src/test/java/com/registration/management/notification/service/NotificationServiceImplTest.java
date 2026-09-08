package com.registration.management.notification.service;

import com.registration.management.audit.entities.AuditLog;
import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.Role;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
import org.mockito.ArgumentCaptor;
import com.registration.management.notification.dto.*;
import com.registration.management.notification.entity.*;
import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationStatus;
import com.registration.management.notification.enums.NotificationType;
import com.registration.management.notification.mapper.NotificationMapper;
import com.registration.management.notification.repository.NotificationLogRepository;
import com.registration.management.notification.repository.NotificationPreferenceRepository;
import com.registration.management.notification.repository.NotificationRepository;
import com.registration.management.notification.repository.NotificationTemplateRepository;
import com.registration.management.notification.sender.NotificationSenderDispatcher;
import com.registration.management.notification.serviceImpl.NotificationServiceImpl;
import com.registration.management.notification.util.TemplateRenderer;
import com.registration.management.registration.repository.RegistrationRepository;
import com.registration.management.school.repository.schoolStaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private schoolStaffRepository schoolStaffRepo;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    private NotificationMapper mapper = new NotificationMapper(new ModelMapper());

    @Spy
    private TemplateRenderer templateRenderer = new TemplateRenderer();

    @Mock
    private NotificationSenderDispatcher senderDispatcher;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User adminUser;
    private User regularUser;
    private User recipientUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        Role adminRole = Role.builder().id(1L).roleCode("ADMIN").roleName("Admin").build();
        Role teacherRole = Role.builder().id(2L).roleCode("LOGIN_TEACHER").roleName("Teacher").build();

        adminUser = User.builder().id(1L).email("admin@test.com").fullName("Admin User").role(adminRole).active(true).build();
        regularUser = User.builder().id(2L).email("teacher@test.com").fullName("Teacher User").role(teacherRole).active(true).build();
        recipientUser = User.builder().id(3L).email("student@test.com").fullName("Student User").role(teacherRole).active(true).build();

        testNotification = Notification.builder()
                .id(101L)
                .recipient(recipientUser)
                .type(NotificationType.REGISTRATION_APPROVED)
                .channel(NotificationChannel.IN_APP)
                .title("Registration Approved")
                .message("Your registration has been approved")
                .status(NotificationStatus.SENT)
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Create notification successfully")
    void testCreateNotification() {
        NotificationRequestDTO request = NotificationRequestDTO.builder()
                .recipientUserId(3L)
                .type(NotificationType.ANNOUNCEMENT)
                .channel(NotificationChannel.IN_APP)
                .title("Test Announcement")
                .message("This is a test announcement")
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(recipientUser));
        when(preferenceRepository.findByUserId(3L)).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(102L);
            return n;
        });

        NotificationResponseDTO response = notificationService.createNotification(request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(102L);
        assertThat(response.getTitle()).isEqualTo("Test Announcement");
        verify(senderDispatcher).dispatch(any(Notification.class));
    }

    @Test
    @DisplayName("Get unread count badge")
    void testGetUnreadCount() {
        when(notificationRepository.countUnreadByRecipientId(regularUser.getId())).thenReturn(5L);

        UnreadCountDTO result = notificationService.getUnreadCount(regularUser);

        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Mark single notification as read")
    void testMarkAsRead() {
        when(notificationRepository.findByIdAndDeletedAtIsNull(101L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponseDTO response = notificationService.markAsRead(101L, recipientUser);

        assertThat(response).isNotNull();
        assertThat(testNotification.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(testNotification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("Non-privileged user cannot access other user's notification")
    void testAccessDeniedForOtherUser() {
        when(notificationRepository.findByIdAndDeletedAtIsNull(101L)).thenReturn(Optional.of(testNotification));

        assertThatThrownBy(() -> notificationService.getNotificationById(101L, regularUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Bulk send notifications by recipient IDs")
    void testBulkNotification() {
        BulkNotificationRequestDTO request = BulkNotificationRequestDTO.builder()
                .recipientUserIds(List.of(2L, 3L))
                .type(NotificationType.ANNOUNCEMENT)
                .channel(NotificationChannel.IN_APP)
                .title("Bulk Notice")
                .message("Notice content")
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(recipientUser));
        when(preferenceRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(new Random().nextLong(1000, 2000));
            return n;
        });

        BulkNotificationResponseDTO response = notificationService.sendBulkNotification(request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.getRequestedCount()).isEqualTo(2);
        assertThat(response.getQueuedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Retry failed notification")
    void testRetryNotification() {
        testNotification.setStatus(NotificationStatus.FAILED);
        testNotification.setFailureReason("Connection timeout");
        when(notificationRepository.findByIdAndDeletedAtIsNull(101L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponseDTO response = notificationService.retryNotification(101L, adminUser);

        assertThat(response).isNotNull();
        assertThat(testNotification.getRetryCount()).isEqualTo(1);
        assertThat(testNotification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        verify(senderDispatcher).dispatch(testNotification);
    }

    @Test
    @DisplayName("Create template with valid parameters")
    void testCreateTemplate() {
        NotificationTemplateRequestDTO request = NotificationTemplateRequestDTO.builder()
                .code("WELCOME_NOTICE")
                .name("Welcome Notice")
                .type(NotificationType.ANNOUNCEMENT)
                .channel(NotificationChannel.EMAIL)
                .subject("Welcome {{recipientName}}")
                .body("Hello {{recipientName}}, welcome to {{schoolName}}")
                .active(true)
                .build();

        when(templateRepository.existsByCodeAndDeletedAtIsNull("WELCOME_NOTICE")).thenReturn(false);
        when(templateRepository.save(any(NotificationTemplate.class))).thenAnswer(invocation -> {
            NotificationTemplate t = invocation.getArgument(0);
            t.setId(50L);
            return t;
        });

        NotificationTemplateResponseDTO response = notificationService.createTemplate(request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("WELCOME_NOTICE");
    }

    @Test
    @DisplayName("Get and update user preferences")
    void testPreferences() {
        when(preferenceRepository.findByUserId(regularUser.getId())).thenReturn(Optional.empty());
        NotificationPreferenceDTO current = notificationService.getPreferences(regularUser);
        assertThat(current.isEmailEnabled()).isTrue();
        assertThat(current.isInAppEnabled()).isTrue();

        NotificationPreferenceDTO updateReq = NotificationPreferenceDTO.builder()
                .emailEnabled(false)
                .smsEnabled(false)
                .inAppEnabled(true)
                .eventEnabled(true)
                .registrationEnabled(false)
                .checkinEnabled(true)
                .build();

        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(invocation -> {
            NotificationPreference p = invocation.getArgument(0);
            p.setId(99L);
            return p;
        });

        NotificationPreferenceDTO updated = notificationService.updatePreferences(updateReq, regularUser);
        assertThat(updated.isEmailEnabled()).isFalse();
        assertThat(updated.isRegistrationEnabled()).isFalse();
    }

    @Test
    @DisplayName("Mark as read saves audit log")
    void testMarkAsReadAuditLog() {
        when(notificationRepository.findByIdAndDeletedAtIsNull(101L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.markAsRead(101L, recipientUser);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.getEntityName()).isEqualTo("Notification");
        assertThat(auditLog.getEntityId()).isEqualTo(101L);
        assertThat(auditLog.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(auditLog.getStatus()).isEqualTo(AuditStatus.SUCCESS);
    }

    @Test
    @DisplayName("Mark all as read saves audit log")
    void testMarkAllAsReadAuditLog() {
        when(notificationRepository.markAllAsReadForRecipient(eq(recipientUser.getId()), any())).thenReturn(5);

        Map<String, Object> result = notificationService.markAllAsRead(recipientUser);

        assertThat(result.get("updatedCount")).isEqualTo(5);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.getEntityName()).isEqualTo("Notification");
        assertThat(auditLog.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(auditLog.getStatus()).isEqualTo(AuditStatus.SUCCESS);
    }

    @Test
    @DisplayName("Delete notification saves audit log")
    void testDeleteNotificationAuditLog() {
        when(notificationRepository.findByIdAndDeletedAtIsNull(101L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.deleteNotification(101L, recipientUser);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.getEntityName()).isEqualTo("Notification");
        assertThat(auditLog.getEntityId()).isEqualTo(101L);
        assertThat(auditLog.getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(auditLog.getStatus()).isEqualTo(AuditStatus.SUCCESS);
    }

    @Test
    @DisplayName("Cancel scheduled notification saves audit log")
    void testCancelScheduledNotificationAuditLog() {
        Notification scheduledNotification = Notification.builder()
                .id(202L)
                .recipient(recipientUser)
                .type(NotificationType.EVENT_REMINDER)
                .channel(NotificationChannel.IN_APP)
                .title("Reminder")
                .status(NotificationStatus.PENDING)
                .scheduledAt(LocalDateTime.now().plusDays(2))
                .build();

        when(notificationRepository.findByIdAndDeletedAtIsNull(202L)).thenReturn(Optional.of(scheduledNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.cancelScheduledNotification(202L, adminUser);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.getEntityName()).isEqualTo("ScheduledNotification");
        assertThat(auditLog.getEntityId()).isEqualTo(202L);
        assertThat(auditLog.getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(auditLog.getStatus()).isEqualTo(AuditStatus.SUCCESS);
    }

    @Test
    @DisplayName("Update template saves audit log")
    void testUpdateTemplateAuditLog() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(10L)
                .code("TEST_TPL")
                .name("Old Name")
                .type(NotificationType.ANNOUNCEMENT)
                .channel(NotificationChannel.IN_APP)
                .body("Hello {{recipientName}}")
                .active(true)
                .build();

        NotificationTemplateRequestDTO req = NotificationTemplateRequestDTO.builder()
                .code("TEST_TPL")
                .name("New Name")
                .type(NotificationType.ANNOUNCEMENT)
                .channel(NotificationChannel.IN_APP)
                .body("Updated body {{recipientName}}")
                .active(true)
                .build();

        when(templateRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.updateTemplate(10L, req, adminUser);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.getEntityName()).isEqualTo("NotificationTemplate");
        assertThat(auditLog.getEntityId()).isEqualTo(10L);
        assertThat(auditLog.getAction()).isEqualTo(AuditAction.UPDATE);
    }

    @Test
    @DisplayName("Delete template saves audit log")
    void testDeleteTemplateAuditLog() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(10L)
                .code("TEST_TPL")
                .name("Test Template")
                .type(NotificationType.ANNOUNCEMENT)
                .channel(NotificationChannel.IN_APP)
                .body("Hello {{recipientName}}")
                .active(true)
                .build();

        when(templateRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.deleteTemplate(10L, adminUser);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.getEntityName()).isEqualTo("NotificationTemplate");
        assertThat(auditLog.getEntityId()).isEqualTo(10L);
        assertThat(auditLog.getAction()).isEqualTo(AuditAction.DELETE);
    }

    @Test
    @DisplayName("Update template status saves audit log")
    void testUpdateTemplateStatusAuditLog() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(10L)
                .code("TEST_TPL")
                .name("Test Template")
                .type(NotificationType.ANNOUNCEMENT)
                .channel(NotificationChannel.IN_APP)
                .body("Hello {{recipientName}}")
                .active(true)
                .build();

        when(templateRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.updateTemplateStatus(10L, TemplateStatusUpdateRequestDTO.builder().active(false).build(), adminUser);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.getEntityName()).isEqualTo("NotificationTemplate");
        assertThat(auditLog.getEntityId()).isEqualTo(10L);
        assertThat(auditLog.getAction()).isEqualTo(AuditAction.UPDATE);
    }
}
