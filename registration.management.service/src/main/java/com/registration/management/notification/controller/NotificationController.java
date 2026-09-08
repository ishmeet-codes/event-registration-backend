package com.registration.management.notification.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.notification.dto.*;
import com.registration.management.notification.enums.NotificationChannel;
import com.registration.management.notification.enums.NotificationStatus;
import com.registration.management.notification.enums.NotificationType;
import com.registration.management.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ─── 1. Create Notification ──────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_CREATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<NotificationResponseDTO> createNotification(
            @Valid @RequestBody NotificationRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        NotificationResponseDTO response = notificationService.createNotification(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── 2. List Notifications (Inbox / Filter) ─────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER') or hasRole('CHECKIN_TEAM')")
    public ResponseEntity<Page<NotificationResponseDTO>> getNotifications(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) Long recipientUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            @AuthenticationPrincipal User currentUser
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<NotificationResponseDTO> result = notificationService.getNotifications(
                search, type, channel, status, read, recipientUserId, dateFrom, dateTo, pageable, currentUser
        );
        return ResponseEntity.ok(result);
    }

    // ─── 3. Notification Details ─────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER') or hasRole('CHECKIN_TEAM')")
    public ResponseEntity<NotificationResponseDTO> getNotificationById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        NotificationResponseDTO response = notificationService.getNotificationById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    // ─── 4. Mark as Read ─────────────────────────────────────────────────────────

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER') or hasRole('CHECKIN_TEAM')")
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        NotificationResponseDTO response = notificationService.markAsRead(id, currentUser);
        return ResponseEntity.ok(response);
    }

    // ─── 5. Mark All Read ────────────────────────────────────────────────────────

    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER') or hasRole('CHECKIN_TEAM')")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> response = notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok(response);
    }

    // ─── 6. Unread Count Badge ───────────────────────────────────────────────────

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER') or hasRole('CHECKIN_TEAM')")
    public ResponseEntity<UnreadCountDTO> getUnreadCount(@AuthenticationPrincipal User currentUser) {
        UnreadCountDTO response = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(response);
    }

    // ─── 7. Delete Notification ──────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_DELETE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER') or hasRole('CHECKIN_TEAM')")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        notificationService.deleteNotification(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // ─── 8. Bulk Notification ────────────────────────────────────────────────────

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('NOTIFICATION_BULK_SEND') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<BulkNotificationResponseDTO> sendBulkNotification(
            @Valid @RequestBody BulkNotificationRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        BulkNotificationResponseDTO response = notificationService.sendBulkNotification(request, currentUser);
        return ResponseEntity.ok(response);
    }

    // ─── 9. Schedule Notification ────────────────────────────────────────────────

    @PostMapping("/schedule")
    @PreAuthorize("hasAuthority('NOTIFICATION_SCHEDULE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<BulkNotificationResponseDTO> scheduleNotification(
            @Valid @RequestBody ScheduleNotificationRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        BulkNotificationResponseDTO response = notificationService.scheduleNotification(request, currentUser);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    // ─── 10. Cancel Scheduled Notification ──────────────────────────────────────

    @DeleteMapping("/scheduled/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_SCHEDULE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<Void> cancelScheduledNotification(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        notificationService.cancelScheduledNotification(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // ─── 11. Scheduled Notifications List ───────────────────────────────────────

    @GetMapping("/scheduled")
    @PreAuthorize("hasAuthority('NOTIFICATION_SCHEDULE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<Page<NotificationResponseDTO>> getScheduledNotifications(
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "scheduledAt,asc") String[] sort,
            @AuthenticationPrincipal User currentUser
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<NotificationResponseDTO> result = notificationService.getScheduledNotifications(
                channel, status, templateCode, scheduledFrom, scheduledTo, pageable, currentUser
        );
        return ResponseEntity.ok(result);
    }

    // ─── 12. Delivery Details ───────────────────────────────────────────────────

    @GetMapping("/delivery/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW_DELIVERY') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<DeliveryDetailsDTO> getDeliveryDetails(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        DeliveryDetailsDTO response = notificationService.getDeliveryDetails(id, currentUser);
        return ResponseEntity.ok(response);
    }

    // ─── 13. Retry Failed Notification ──────────────────────────────────────────

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('NOTIFICATION_RETRY') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<NotificationResponseDTO> retryNotification(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        NotificationResponseDTO response = notificationService.retryNotification(id, currentUser);
        return ResponseEntity.ok(response);
    }

    // ─── 14. List Templates ─────────────────────────────────────────────────────

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('NOTIFICATION_TEMPLATE_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<Page<NotificationTemplateResponseDTO>> getTemplates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<NotificationTemplateResponseDTO> result = notificationService.getTemplates(search, type, channel, active, pageable);
        return ResponseEntity.ok(result);
    }

    // ─── 15. Create Template ────────────────────────────────────────────────────

    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('NOTIFICATION_TEMPLATE_CREATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<NotificationTemplateResponseDTO> createTemplate(
            @Valid @RequestBody NotificationTemplateRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        NotificationTemplateResponseDTO response = notificationService.createTemplate(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── 16. Template Details ───────────────────────────────────────────────────

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_TEMPLATE_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<NotificationTemplateResponseDTO> getTemplateById(@PathVariable("id") Long id) {
        NotificationTemplateResponseDTO response = notificationService.getTemplateById(id);
        return ResponseEntity.ok(response);
    }

    // ─── 17. Update Template ────────────────────────────────────────────────────

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_TEMPLATE_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<NotificationTemplateResponseDTO> updateTemplate(
            @PathVariable("id") Long id,
            @Valid @RequestBody NotificationTemplateRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        NotificationTemplateResponseDTO response = notificationService.updateTemplate(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    // ─── 18. Delete Template ────────────────────────────────────────────────────

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_TEMPLATE_DELETE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        notificationService.deleteTemplate(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // ─── 19. Update Template Status ─────────────────────────────────────────────

    @PatchMapping("/templates/{id}/status")
    @PreAuthorize("hasAuthority('NOTIFICATION_TEMPLATE_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<NotificationTemplateResponseDTO> updateTemplateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody TemplateStatusUpdateRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        NotificationTemplateResponseDTO response = notificationService.updateTemplateStatus(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    // ─── 20. Get Own Preferences ────────────────────────────────────────────────

    @GetMapping("/preferences")
    @PreAuthorize("hasAuthority('NOTIFICATION_PREFERENCE_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER') or hasRole('CHECKIN_TEAM')")
    public ResponseEntity<NotificationPreferenceDTO> getPreferences(@AuthenticationPrincipal User currentUser) {
        NotificationPreferenceDTO response = notificationService.getPreferences(currentUser);
        return ResponseEntity.ok(response);
    }

    // ─── 21. Update Own Preferences ─────────────────────────────────────────────

    @PutMapping("/preferences")
    @PreAuthorize("hasAuthority('NOTIFICATION_PREFERENCE_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER') or hasRole('CHECKIN_TEAM')")
    public ResponseEntity<NotificationPreferenceDTO> updatePreferences(
            @Valid @RequestBody NotificationPreferenceDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        NotificationPreferenceDTO response = notificationService.updatePreferences(request, currentUser);
        return ResponseEntity.ok(response);
    }

    // ─── 22. Notification Summary / Analytics ───────────────────────────────────

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW_ANALYTICS') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<NotificationSummaryDTO> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationType type
    ) {
        NotificationSummaryDTO response = notificationService.getSummary(dateFrom, dateTo, channel, type);
        return ResponseEntity.ok(response);
    }

    // ─── 23. Delivery / Notification Logs ───────────────────────────────────────

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW_LOGS') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<NotificationLogDTO>> getLogs(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Page<NotificationLogDTO> result = notificationService.getLogs(status, channel, type, recipient, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(result);
    }

    // ─── Helper: Pageable Builder ───────────────────────────────────────────────

    private Pageable createPageable(int page, int size, String[] sort) {
        if (size > 100) {
            size = 100;
        }
        if (size < 1) {
            size = 20;
        }
        if (page < 0) {
            page = 0;
        }

        Sort sortObj = Sort.unsorted();
        if (sort != null && sort.length > 0) {
            String property = sort[0];
            Sort.Direction direction = Sort.Direction.ASC;
            if (sort.length > 1 && "desc".equalsIgnoreCase(sort[1])) {
                direction = Sort.Direction.DESC;
            } else if (property.contains(",")) {
                String[] parts = property.split(",");
                property = parts[0];
                if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1])) {
                    direction = Sort.Direction.DESC;
                }
            }
            sortObj = Sort.by(direction, property);
        }

        return PageRequest.of(page, size, sortObj);
    }
}
