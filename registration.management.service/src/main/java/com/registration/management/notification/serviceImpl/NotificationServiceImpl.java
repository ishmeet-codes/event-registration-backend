package com.registration.management.notification.serviceImpl;

import com.registration.management.audit.entities.AuditLog;
import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.common.exception.ResourceNotFoundException;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
import com.registration.management.notification.dto.*;
import com.registration.management.notification.entity.*;
import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationStatus;
import com.registration.management.notification.enums.NotificationType;
import com.registration.management.notification.enums.RecipientTargetType;
import com.registration.management.notification.event.DomainNotificationEvent;
import com.registration.management.notification.mapper.NotificationMapper;
import com.registration.management.notification.repository.NotificationLogRepository;
import com.registration.management.notification.repository.NotificationPreferenceRepository;
import com.registration.management.notification.repository.NotificationTemplateRepository;
import com.registration.management.notification.repository.NotificationRepository;
import com.registration.management.notification.sender.NotificationSenderDispatcher;
import com.registration.management.notification.service.NotificationService;
import com.registration.management.notification.util.TemplateRenderer;
import com.registration.management.registration.entity.Registration;
import com.registration.management.registration.repository.RegistrationRepository;
import com.registration.management.school.entity.SchoolStaff;
import com.registration.management.school.repository.schoolStaffRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationLogRepository logRepository;
    private final UserRepository userRepository;
    private final schoolStaffRepository schoolStaffRepo;
    private final RegistrationRepository registrationRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationMapper mapper;
    private final TemplateRenderer templateRenderer;
    private final NotificationSenderDispatcher senderDispatcher;

    // ─── 1. Create Notification ──────────────────────────────────────────────────

    @Override
    @Transactional
    public NotificationResponseDTO createNotification(NotificationRequestDTO request, User currentUser) {
        if (request.getRecipientUserId() == null) {
            throw new IllegalArgumentException("recipientUserId is required");
        }
        User recipient = userRepository.findById(request.getRecipientUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found with id: " + request.getRecipientUserId()));

        // Check preference
        if (!isChannelAndTypeEnabledForUser(recipient.getId(), request.getChannel(), request.getType())) {
            log.info("Notification creation skipped: recipient id={} opted out of channel={} or type={}",
                    recipient.getId(), request.getChannel(), request.getType());
        }

        String title = request.getTitle();
        String message = request.getMessage();

        // Check if template requested
        if (request.getTemplateCode() != null && !request.getTemplateCode().isBlank()) {
            NotificationTemplate template = templateRepository.findByCodeAndDeletedAtIsNull(request.getTemplateCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found with code: " + request.getTemplateCode()));
            if (!template.isActive()) {
                throw new IllegalStateException("Template " + template.getCode() + " is currently inactive");
            }
            Map<String, Object> vars = request.getTemplateVariables() != null ? new HashMap<>(request.getTemplateVariables()) : new HashMap<>();
            vars.putIfAbsent("recipientName", recipient.getFullName());
            vars.putIfAbsent("recipientEmail", recipient.getEmail());

            title = template.getSubject() != null ? templateRenderer.render(template.getSubject(), vars) : title;
            message = templateRenderer.render(template.getBody(), vars);
        }

        if (title == null || title.isBlank()) {
            title = defaultTitleForType(request.getType());
        }
        if (message == null || message.isBlank()) {
            message = "You have a new update regarding " + request.getType().name();
        }

        // Idempotency check
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            if (notificationRepository.existsByIdempotencyKeyAndStatusNot(request.getIdempotencyKey(), NotificationStatus.FAILED)) {
                throw new IllegalStateException("Duplicate notification request for idempotencyKey: " + request.getIdempotencyKey());
            }
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(request.getType())
                .channel(request.getChannel())
                .title(title)
                .message(message)
                .status(NotificationStatus.PENDING)
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .idempotencyKey(request.getIdempotencyKey())
                .retryCount(0)
                .maxRetries(3)
                .build();

        notification = notificationRepository.save(notification);

        // Dispatch immediately
        senderDispatcher.dispatch(notification);

        saveAuditLog(currentUser, "Notification", notification.getId(), AuditAction.CREATE, AuditStatus.SUCCESS, null);
        return mapper.toNotificationResponseDTO(notification);
    }

    // ─── 2. List Notifications (Inbox / Filter) ─────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getNotifications(
            String search,
            NotificationType type,
            NotificationChannel channel,
            NotificationStatus status,
            Boolean read,
            Long recipientUserId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable,
            User currentUser
    ) {
        boolean isAdminOrManager = isPrivileged(currentUser);
        Long targetRecipientId;

        if (isAdminOrManager) {
            targetRecipientId = recipientUserId; // can filter by any or all
        } else {
            // Regular user can only view their own
            targetRecipientId = currentUser.getId();
        }

        Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (targetRecipientId != null) {
                predicates.add(cb.equal(root.get("recipient").get("id"), targetRecipientId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (read != null) {
                if (read) {
                    predicates.add(cb.or(
                            cb.isNotNull(root.get("readAt")),
                            cb.equal(root.get("status"), NotificationStatus.READ)
                    ));
                } else {
                    predicates.add(cb.and(
                            cb.isNull(root.get("readAt")),
                            cb.notEqual(root.get("status"), NotificationStatus.READ)
                    ));
                }
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }
            if (search != null && !search.isBlank()) {
                String term = "%" + search.toLowerCase().trim() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("title")), term),
                        cb.like(cb.lower(root.get("message")), term),
                        cb.like(cb.lower(root.get("recipient").get("fullName")), term),
                        cb.like(cb.lower(root.get("recipient").get("email")), term)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return notificationRepository.findAll(spec, pageable).map(mapper::toNotificationResponseDTO);
    }

    // ─── 3. Notification Details ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public NotificationResponseDTO getNotificationById(Long id, User currentUser) {
        Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        validateRecipientOrPrivileged(notification, currentUser);
        return mapper.toNotificationResponseDTO(notification);
    }

    // ─── 4. Mark as Read ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public NotificationResponseDTO markAsRead(Long id, User currentUser) {
        Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        validateRecipientOrPrivileged(notification, currentUser);

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification.setStatus(NotificationStatus.READ);
            notification = notificationRepository.save(notification);
        }
        return mapper.toNotificationResponseDTO(notification);
    }

    // ─── 5. Mark All Read ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Map<String, Object> markAllAsRead(User currentUser) {
        int updatedCount = notificationRepository.markAllAsReadForRecipient(currentUser.getId(), LocalDateTime.now());
        Map<String, Object> response = new HashMap<>();
        response.put("updatedCount", updatedCount);
        response.put("message", "Marked " + updatedCount + " notifications as read");
        return response;
    }

    // ─── 6. Unread Count Badge ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UnreadCountDTO getUnreadCount(User currentUser) {
        long count = notificationRepository.countUnreadByRecipientId(currentUser.getId());
        return UnreadCountDTO.builder().count(count).build();
    }

    // ─── 7. Delete Notification (Soft Delete) ───────────────────────────────────

    @Override
    @Transactional
    public void deleteNotification(Long id, User currentUser) {
        Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        validateRecipientOrPrivileged(notification, currentUser);

        notification.setDeletedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        saveAuditLog(currentUser, "Notification", id, AuditAction.DELETE, AuditStatus.SUCCESS, null);
    }

    // ─── 8. Bulk Notification ────────────────────────────────────────────────────

    @Override
    @Transactional
    public BulkNotificationResponseDTO sendBulkNotification(BulkNotificationRequestDTO request, User currentUser) {
        Set<Long> targetUserIds = resolveRecipientIds(request.getRecipientUserIds(), request.getTargetType(), request.getTargetId());
        if (targetUserIds.isEmpty()) {
            throw new IllegalArgumentException("No eligible recipients found for bulk notification request");
        }

        List<Long> queuedIds = new ArrayList<>();
        int failedCount = 0;

        for (Long recipientId : targetUserIds) {
            try {
                NotificationRequestDTO singleReq = NotificationRequestDTO.builder()
                        .recipientUserId(recipientId)
                        .type(request.getType() != null ? request.getType() : NotificationType.ANNOUNCEMENT)
                        .channel(request.getChannel())
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .templateCode(request.getTemplateCode())
                        .templateVariables(request.getTemplateVariables())
                        .build();

                NotificationResponseDTO created = createNotification(singleReq, currentUser);
                queuedIds.add(created.getId());
            } catch (Exception e) {
                log.error("Failed to enqueue bulk notification for recipientId={}: {}", recipientId, e.getMessage());
                failedCount++;
            }
        }

        saveAuditLog(currentUser, "BulkNotification", null, AuditAction.CREATE, AuditStatus.SUCCESS,
                "Queued " + queuedIds.size() + " bulk notifications");

        return BulkNotificationResponseDTO.builder()
                .requestedCount(targetUserIds.size())
                .queuedCount(queuedIds.size())
                .failedCount(failedCount)
                .notificationIds(queuedIds)
                .message("Bulk notification processing completed: " + queuedIds.size() + " queued, " + failedCount + " failed")
                .build();
    }

    // ─── 9. Schedule Notification ────────────────────────────────────────────────

    @Override
    @Transactional
    public BulkNotificationResponseDTO scheduleNotification(ScheduleNotificationRequestDTO request, User currentUser) {
        if (request.getScheduledAt() == null || request.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("scheduledAt must be a valid future timestamp");
        }

        Set<Long> targetUserIds = resolveRecipientIds(request.getRecipientUserIds(), request.getTargetType(), request.getTargetId());
        if (targetUserIds.isEmpty()) {
            throw new IllegalArgumentException("No eligible recipients found for scheduled notification request");
        }

        List<Long> scheduledIds = new ArrayList<>();
        int failedCount = 0;

        for (Long recipientId : targetUserIds) {
            try {
                User recipient = userRepository.findById(recipientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found: " + recipientId));

                String title = request.getTitle();
                String message = request.getMessage();

                if (request.getTemplateCode() != null && !request.getTemplateCode().isBlank()) {
                    NotificationTemplate template = templateRepository.findByCodeAndDeletedAtIsNull(request.getTemplateCode())
                            .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + request.getTemplateCode()));
                    Map<String, Object> vars = request.getTemplateVariables() != null ? new HashMap<>(request.getTemplateVariables()) : new HashMap<>();
                    vars.putIfAbsent("recipientName", recipient.getFullName());
                    vars.putIfAbsent("recipientEmail", recipient.getEmail());
                    title = template.getSubject() != null ? templateRenderer.render(template.getSubject(), vars) : title;
                    message = templateRenderer.render(template.getBody(), vars);
                }

                if (title == null || title.isBlank()) {
                    title = defaultTitleForType(request.getType());
                }
                if (message == null || message.isBlank()) {
                    message = "Scheduled notification update";
                }

                Notification notification = Notification.builder()
                        .recipient(recipient)
                        .type(request.getType() != null ? request.getType() : NotificationType.EVENT_REMINDER)
                        .channel(request.getChannel())
                        .title(title)
                        .message(message)
                        .status(NotificationStatus.PENDING)
                        .scheduledAt(request.getScheduledAt())
                        .retryCount(0)
                        .maxRetries(3)
                        .build();

                notification = notificationRepository.save(notification);
                scheduledIds.add(notification.getId());
            } catch (Exception e) {
                log.error("Failed to schedule notification for user {}: {}", recipientId, e.getMessage());
                failedCount++;
            }
        }

        saveAuditLog(currentUser, "ScheduledNotification", null, AuditAction.CREATE, AuditStatus.SUCCESS,
                "Scheduled " + scheduledIds.size() + " notifications for " + request.getScheduledAt());

        return BulkNotificationResponseDTO.builder()
                .requestedCount(targetUserIds.size())
                .queuedCount(scheduledIds.size())
                .failedCount(failedCount)
                .notificationIds(scheduledIds)
                .message("Scheduled " + scheduledIds.size() + " notifications for delivery at " + request.getScheduledAt())
                .build();
    }

    // ─── 10. Cancel Scheduled Notification ──────────────────────────────────────

    @Override
    @Transactional
    public void cancelScheduledNotification(Long id, User currentUser) {
        Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled notification not found with id: " + id));

        if (notification.getScheduledAt() == null || notification.getStatus() != NotificationStatus.PENDING) {
            throw new IllegalStateException("Only pending scheduled notifications can be cancelled. Current status: " + notification.getStatus());
        }

        notification.setStatus(NotificationStatus.CANCELLED);
        notification.setFailureReason("Cancelled by user " + currentUser.getEmail());
        notificationRepository.save(notification);

        saveAuditLog(currentUser, "ScheduledNotification", id, AuditAction.DELETE, AuditStatus.SUCCESS, "Cancelled scheduled notification");
    }

    // ─── 11. Scheduled Notifications List ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getScheduledNotifications(
            NotificationChannel channel,
            NotificationStatus status,
            String templateCode,
            LocalDateTime scheduledFrom,
            LocalDateTime scheduledTo,
            Pageable pageable,
            User currentUser
    ) {
        Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.isNotNull(root.get("scheduledAt")));

            if (!isPrivileged(currentUser)) {
                predicates.add(cb.equal(root.get("recipient").get("id"), currentUser.getId()));
            }

            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (scheduledFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledAt"), scheduledFrom));
            }
            if (scheduledTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scheduledAt"), scheduledTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return notificationRepository.findAll(spec, pageable).map(mapper::toNotificationResponseDTO);
    }

    // ─── 12. Delivery Details ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DeliveryDetailsDTO getDeliveryDetails(Long id, User currentUser) {
        Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        validateRecipientOrPrivileged(notification, currentUser);

        NotificationLog latestLog = logRepository.findTopByNotificationIdOrderByCreatedAtDesc(id).orElse(null);

        return DeliveryDetailsDTO.builder()
                .notificationId(notification.getId())
                .channel(notification.getChannel())
                .status(notification.getStatus())
                .attempts(notification.getRetryCount() + 1)
                .sentAt(notification.getSentAt())
                .deliveredAt(notification.getDeliveredAt())
                .failureReason(notification.getFailureReason())
                .recipientEmail(notification.getRecipient() != null ? notification.getRecipient().getEmail() : null)
                .recipientPhone(latestLog != null ? latestLog.getRecipientPhone() : null)
                .build();
    }

    // ─── 13. Retry Failed Notification ──────────────────────────────────────────

    @Override
    @Transactional
    public NotificationResponseDTO retryNotification(Long id, User currentUser) {
        Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new IllegalStateException("Only FAILED notifications can be retried. Current status: " + notification.getStatus());
        }

        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            throw new IllegalStateException("Maximum retry attempts (" + notification.getMaxRetries() + ") reached for notification id: " + id);
        }

        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setFailureReason(null);
        notification = notificationRepository.save(notification);

        // Dispatch
        senderDispatcher.dispatch(notification);

        saveAuditLog(currentUser, "Notification", id, AuditAction.UPDATE, AuditStatus.SUCCESS, "Retried failed notification attempt #" + notification.getRetryCount());
        return mapper.toNotificationResponseDTO(notification);
    }

    // ─── 14. List Templates ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationTemplateResponseDTO> getTemplates(
            String search,
            NotificationType type,
            NotificationChannel channel,
            Boolean active,
            Pageable pageable
    ) {
        Specification<NotificationTemplate> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (search != null && !search.isBlank()) {
                String term = "%" + search.toLowerCase().trim() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("name")), term),
                        cb.like(cb.lower(root.get("code")), term),
                        cb.like(cb.lower(root.get("subject")), term)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return templateRepository.findAll(spec, pageable).map(mapper::toTemplateResponseDTO);
    }

    // ─── 15. Create Template ────────────────────────────────────────────────────

    @Override
    @Transactional
    public NotificationTemplateResponseDTO createTemplate(NotificationTemplateRequestDTO request, User currentUser) {
        if (templateRepository.existsByCodeAndDeletedAtIsNull(request.getCode())) {
            throw new IllegalArgumentException("Template code '" + request.getCode() + "' already exists");
        }

        templateRenderer.validateTemplate(request.getSubject());
        templateRenderer.validateTemplate(request.getBody());

        if (request.getChannel() == NotificationChannel.EMAIL && (request.getSubject() == null || request.getSubject().isBlank())) {
            throw new IllegalArgumentException("Subject is required for EMAIL channel templates");
        }

        NotificationTemplate template = NotificationTemplate.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .type(request.getType())
                .channel(request.getChannel())
                .subject(request.getSubject())
                .body(request.getBody())
                .active(request.isActive())
                .build();

        template = templateRepository.save(template);
        saveAuditLog(currentUser, "NotificationTemplate", template.getId(), AuditAction.CREATE, AuditStatus.SUCCESS, "Created template " + template.getCode());
        return mapper.toTemplateResponseDTO(template);
    }

    // ─── 16. Template Details ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponseDTO getTemplateById(Long id) {
        NotificationTemplate template = templateRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));
        return mapper.toTemplateResponseDTO(template);
    }

    // ─── 17. Update Template ────────────────────────────────────────────────────

    @Override
    @Transactional
    public NotificationTemplateResponseDTO updateTemplate(Long id, NotificationTemplateRequestDTO request, User currentUser) {
        NotificationTemplate template = templateRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        if (!template.getCode().equalsIgnoreCase(request.getCode()) && templateRepository.existsByCodeAndDeletedAtIsNull(request.getCode())) {
            throw new IllegalArgumentException("Template with code '" + request.getCode() + "' already exists");
        }

        templateRenderer.validateTemplate(request.getSubject());
        templateRenderer.validateTemplate(request.getBody());

        if (request.getChannel() == NotificationChannel.EMAIL && (request.getSubject() == null || request.getSubject().isBlank())) {
            throw new IllegalArgumentException("Subject is required for EMAIL channel templates");
        }

        template.setCode(request.getCode().trim().toUpperCase());
        template.setName(request.getName().trim());
        template.setType(request.getType());
        template.setChannel(request.getChannel());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        template.setActive(request.isActive());

        template = templateRepository.save(template);
        saveAuditLog(currentUser, "NotificationTemplate", id, AuditAction.UPDATE, AuditStatus.SUCCESS, "Updated template " + template.getCode());
        return mapper.toTemplateResponseDTO(template);
    }

    // ─── 18. Delete Template ────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteTemplate(Long id, User currentUser) {
        NotificationTemplate template = templateRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        template.setDeletedAt(LocalDateTime.now());
        template.setActive(false);
        templateRepository.save(template);

        saveAuditLog(currentUser, "NotificationTemplate", id, AuditAction.DELETE, AuditStatus.SUCCESS, "Deleted template " + template.getCode());
    }

    // ─── 19. Update Template Status ─────────────────────────────────────────────

    @Override
    @Transactional
    public NotificationTemplateResponseDTO updateTemplateStatus(Long id, TemplateStatusUpdateRequestDTO request, User currentUser) {
        NotificationTemplate template = templateRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        template.setActive(request.getActive());
        template = templateRepository.save(template);

        saveAuditLog(currentUser, "NotificationTemplate", id, AuditAction.UPDATE, AuditStatus.SUCCESS, "Set template active=" + request.getActive());
        return mapper.toTemplateResponseDTO(template);
    }

    // ─── 20. Get Preferences ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceDTO getPreferences(User currentUser) {
        NotificationPreference pref = preferenceRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> NotificationPreference.builder()
                        .user(currentUser)
                        .emailEnabled(true)
                        .smsEnabled(false)
                        .inAppEnabled(true)
                        .eventEnabled(true)
                        .registrationEnabled(true)
                        .checkinEnabled(true)
                        .build());
        return mapper.toPreferenceDTO(pref);
    }

    // ─── 21. Update Preferences ─────────────────────────────────────────────────

    @Override
    @Transactional
    public NotificationPreferenceDTO updatePreferences(NotificationPreferenceDTO request, User currentUser) {
        NotificationPreference pref = preferenceRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> NotificationPreference.builder().user(currentUser).build());

        pref.setEmailEnabled(request.isEmailEnabled());
        pref.setSmsEnabled(request.isSmsEnabled());
        pref.setInAppEnabled(request.isInAppEnabled());
        pref.setEventEnabled(request.isEventEnabled());
        pref.setRegistrationEnabled(request.isRegistrationEnabled());
        pref.setCheckinEnabled(request.isCheckinEnabled());

        pref = preferenceRepository.save(pref);
        saveAuditLog(currentUser, "NotificationPreference", pref.getId(), AuditAction.UPDATE, AuditStatus.SUCCESS, "Updated preferences");
        return mapper.toPreferenceDTO(pref);
    }

    // ─── 22. Summary / Analytics ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public NotificationSummaryDTO getSummary(
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            NotificationChannel channel,
            NotificationType type
    ) {
        long total = notificationRepository.countSummaryTotal(dateFrom, dateTo, channel, type);
        long sent = notificationRepository.countSummaryByStatus(NotificationStatus.SENT, dateFrom, dateTo, channel, type);
        long delivered = notificationRepository.countSummaryByStatus(NotificationStatus.DELIVERED, dateFrom, dateTo, channel, type);
        long failed = notificationRepository.countSummaryByStatus(NotificationStatus.FAILED, dateFrom, dateTo, channel, type);
        long read = notificationRepository.countSummaryRead(dateFrom, dateTo, channel, type);
        long pending = notificationRepository.countSummaryByStatus(NotificationStatus.PENDING, dateFrom, dateTo, channel, type);
        long scheduled = notificationRepository.countSummaryScheduled(LocalDateTime.now(), dateFrom, dateTo, channel, type);
        long cancelled = notificationRepository.countSummaryByStatus(NotificationStatus.CANCELLED, dateFrom, dateTo, channel, type);

        return NotificationSummaryDTO.builder()
                .total(total)
                .sent(sent)
                .delivered(delivered)
                .failed(failed)
                .read(read)
                .pending(pending)
                .scheduled(scheduled)
                .cancelled(cancelled)
                .build();
    }

    // ─── 23. Delivery / Notification Logs ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationLogDTO> getLogs(
            NotificationStatus status,
            NotificationChannel channel,
            NotificationType type,
            String recipient,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    ) {
        Specification<NotificationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("notification").get("type"), type));
            }
            if (recipient != null && !recipient.isBlank()) {
                String term = "%" + recipient.toLowerCase().trim() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("recipientEmail")), term),
                        cb.like(cb.lower(root.get("recipientPhone")), term),
                        cb.like(cb.lower(root.get("notification").get("recipient").get("fullName")), term)
                ));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return logRepository.findAll(spec, pageable).map(mapper::toLogDTO);
    }

    // ─── Background Domain Notification Handler ──────────────────────────────────

    @Override
    @Transactional
    public void sendDomainNotification(DomainNotificationEvent event) {
        if (event == null || event.getRecipientUserId() == null) {
            return;
        }

        User recipient = userRepository.findById(event.getRecipientUserId()).orElse(null);
        if (recipient == null) {
            return;
        }

        NotificationPreference pref = preferenceRepository.findByUserId(recipient.getId()).orElse(null);

        // Check topic preference
        if (pref != null) {
            if (isEventTopic(event.getType()) && !pref.isEventEnabled()) return;
            if (isRegistrationTopic(event.getType()) && !pref.isRegistrationEnabled()) return;
            if (isCheckinTopic(event.getType()) && !pref.isCheckinEnabled()) return;
        }

        // Send In-App notification if enabled
        if (pref == null || pref.isInAppEnabled()) {
            createAndDispatchDomainNotification(event, recipient, NotificationChannel.IN_APP);
        }

        // Send Email notification if enabled
        if (pref == null || pref.isEmailEnabled()) {
            createAndDispatchDomainNotification(event, recipient, NotificationChannel.EMAIL);
        }
    }

    private void createAndDispatchDomainNotification(DomainNotificationEvent event, User recipient, NotificationChannel channel) {
        try {
            String title = event.getTitle();
            String message = event.getMessage();

            // Try template lookup
            Optional<NotificationTemplate> templateOpt = Optional.empty();
            if (event.getTemplateCode() != null && !event.getTemplateCode().isBlank()) {
                templateOpt = templateRepository.findByCodeAndDeletedAtIsNull(event.getTemplateCode());
            }
            if (templateOpt.isEmpty()) {
                templateOpt = templateRepository.findByTypeAndChannelAndActiveTrueAndDeletedAtIsNull(event.getType(), channel);
            }

            if (templateOpt.isPresent() && templateOpt.get().isActive()) {
                NotificationTemplate template = templateOpt.get();
                Map<String, Object> vars = event.getVariables() != null ? new HashMap<>(event.getVariables()) : new HashMap<>();
                vars.putIfAbsent("recipientName", recipient.getFullName());
                vars.putIfAbsent("recipientEmail", recipient.getEmail());

                if (template.getSubject() != null && !template.getSubject().isBlank()) {
                    title = templateRenderer.render(template.getSubject(), vars);
                }
                message = templateRenderer.render(template.getBody(), vars);
            }

            if (title == null || title.isBlank()) {
                title = defaultTitleForType(event.getType());
            }
            if (message == null || message.isBlank()) {
                message = "You have an update regarding " + event.getType().name();
            }

            String idempotencyKey = event.getIdempotencyKey();
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyKey = idempotencyKey + "_" + channel.name();
                if (notificationRepository.existsByIdempotencyKeyAndStatusNot(idempotencyKey, NotificationStatus.FAILED)) {
                    return;
                }
            }

            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .type(event.getType())
                    .channel(channel)
                    .title(title)
                    .message(message)
                    .status(NotificationStatus.PENDING)
                    .referenceType(event.getReferenceType())
                    .referenceId(event.getReferenceId())
                    .idempotencyKey(idempotencyKey)
                    .retryCount(0)
                    .maxRetries(3)
                    .build();

            notification = notificationRepository.save(notification);
            senderDispatcher.dispatch(notification);
        } catch (Exception ex) {
            log.error("Failed to generate domain notification for recipient={} channel={}: {}", recipient.getId(), channel, ex.getMessage());
        }
    }

    // ─── Scheduled Notification Runner ──────────────────────────────────────────

    @Override
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> readyList = notificationRepository.findReadyScheduledNotifications(now);
        if (readyList.isEmpty()) {
            return;
        }
        log.info("Processing {} ready scheduled notifications", readyList.size());
        for (Notification notification : readyList) {
            try {
                senderDispatcher.dispatch(notification);
            } catch (Exception ex) {
                log.error("Error dispatching scheduled notification id={}: {}", notification.getId(), ex.getMessage());
            }
        }
    }

    // ─── Helper Methods ─────────────────────────────────────────────────────────

    private boolean isPrivileged(User user) {
        if (user == null || user.getRole() == null) return false;
        String code = user.getRole().getRoleCode();
        return "SUPER_ADMIN".equalsIgnoreCase(code) || "ADMIN".equalsIgnoreCase(code) || "EVENT_MANAGER".equalsIgnoreCase(code);
    }

    private void validateRecipientOrPrivileged(Notification notification, User currentUser) {
        if (isPrivileged(currentUser)) {
            return;
        }
        if (notification.getRecipient() == null || !notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You can only view and manage your own notifications");
        }
    }

    private boolean isChannelAndTypeEnabledForUser(Long userId, NotificationChannel channel, NotificationType type) {
        Optional<NotificationPreference> prefOpt = preferenceRepository.findByUserId(userId);
        if (prefOpt.isEmpty()) {
            return true;
        }
        NotificationPreference pref = prefOpt.get();
        if (channel == NotificationChannel.EMAIL && !pref.isEmailEnabled()) return false;
        if (channel == NotificationChannel.IN_APP && !pref.isInAppEnabled()) return false;
        if (channel == NotificationChannel.SMS && !pref.isSmsEnabled()) return false;

        if (isEventTopic(type) && !pref.isEventEnabled()) return false;
        if (isRegistrationTopic(type) && !pref.isRegistrationEnabled()) return false;
        if (isCheckinTopic(type) && !pref.isCheckinEnabled()) return false;

        return true;
    }

    private boolean isEventTopic(NotificationType type) {
        return type == NotificationType.EVENT_CREATED || type == NotificationType.EVENT_UPDATED
                || type == NotificationType.EVENT_CANCELLED || type == NotificationType.EVENT_REMINDER
                || type == NotificationType.EVENT_STARTED || type == NotificationType.EVENT_COMPLETED;
    }

    private boolean isRegistrationTopic(NotificationType type) {
        return type == NotificationType.REGISTRATION_SUBMITTED || type == NotificationType.REGISTRATION_APPROVED
                || type == NotificationType.REGISTRATION_REJECTED || type == NotificationType.REGISTRATION_UPDATED
                || type == NotificationType.REGISTRATION_CANCELLED;
    }

    private boolean isCheckinTopic(NotificationType type) {
        return type == NotificationType.CHECKIN_CONFIRMED || type == NotificationType.CHECKOUT_CONFIRMED;
    }

    private String defaultTitleForType(NotificationType type) {
        if (type == null) return "Notification";
        return switch (type) {
            case REGISTRATION_SUBMITTED -> "Registration Submitted";
            case REGISTRATION_APPROVED -> "Registration Approved";
            case REGISTRATION_REJECTED -> "Registration Rejected";
            case REGISTRATION_UPDATED -> "Registration Updated";
            case REGISTRATION_CANCELLED -> "Registration Cancelled";
            case EVENT_CREATED -> "New Event Created";
            case EVENT_UPDATED -> "Event Details Updated";
            case EVENT_CANCELLED -> "Event Cancelled";
            case EVENT_REMINDER -> "Upcoming Event Reminder";
            case EVENT_STARTED -> "Event Has Started";
            case EVENT_COMPLETED -> "Event Completed";
            case PARTICIPANT_ADDED -> "Participant Added";
            case PARTICIPANT_UPDATED -> "Participant Updated";
            case PARTICIPANT_REMOVED -> "Participant Removed";
            case CHECKIN_CONFIRMED -> "Check-in Confirmed";
            case CHECKOUT_CONFIRMED -> "Check-out Confirmed";
            case ANNOUNCEMENT -> "Announcement";
            case SYSTEM_NOTIFICATION -> "System Notification";
        };
    }

    private Set<Long> resolveRecipientIds(List<Long> directUserIds, RecipientTargetType targetType, Long targetId) {
        Set<Long> userIds = new HashSet<>();
        if (directUserIds != null && !directUserIds.isEmpty()) {
            userIds.addAll(directUserIds);
        }

        if (targetType != null) {
            switch (targetType) {
                case ALL_USERS -> {
                    List<User> users = userRepository.findByActiveTrue();
                    users.forEach(u -> userIds.add(u.getId()));
                }
                case ALL_STAFF -> {
                    List<User> staffUsers = userRepository.findByRoleRoleCodeInAndActiveTrue(
                            List.of("SCHOOL_STAFF", "LOGIN_TEACHER", "ACCOMPANYING_TEACHER", "CHECKIN_TEAM")
                    );
                    staffUsers.forEach(u -> userIds.add(u.getId()));
                }
                case ALL_TEACHERS -> {
                    List<User> teachers = userRepository.findByRoleRoleCodeInAndActiveTrue(
                            List.of("LOGIN_TEACHER", "ACCOMPANYING_TEACHER")
                    );
                    teachers.forEach(u -> userIds.add(u.getId()));
                }
                case SCHOOL -> {
                    if (targetId != null) {
                        List<SchoolStaff> staffMembers = schoolStaffRepo.findBySchoolId(targetId);
                        for (SchoolStaff ss : staffMembers) {
                            if (ss.getUser() != null) {
                                userIds.add(ss.getUser().getId());
                            }
                        }
                    }
                }
                case EVENT -> {
                    if (targetId != null) {
                        // find users associated with registrations for event
                        List<Registration> registrations = registrationRepository.findByEventId(targetId);
                        for (Registration reg : registrations) {
                            if (reg.getCreatedByStaff() != null && reg.getCreatedByStaff().getUser() != null) {
                                userIds.add(reg.getCreatedByStaff().getUser().getId());
                            }
                        }
                    }
                }
                case REGISTRATION -> {
                    if (targetId != null) {
                        registrationRepository.findById(targetId).ifPresent(reg -> {
                            if (reg.getCreatedByStaff() != null && reg.getCreatedByStaff().getUser() != null) {
                                userIds.add(reg.getCreatedByStaff().getUser().getId());
                            }
                        });
                    }
                }
            }
        }

        return userIds;
    }

    private void saveAuditLog(User user, String entityName, Long entityId, AuditAction action, AuditStatus status, String errorMessage) {
        try {
            AuditLog log = AuditLog.builder()
                    .user(user)
                    .entityName(entityName)
                    .entityId(entityId)
                    .action(action)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            log.warn("Failed to write notification audit log: {}", e.getMessage());
        }
    }
}
