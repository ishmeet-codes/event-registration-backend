package com.registration.management.notification.event;

import com.registration.management.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainNotificationEvent {
    private NotificationType type;
    private Long recipientUserId;
    private String title;
    private String message;
    private String referenceType;
    private Long referenceId;
    private String templateCode;
    private Map<String, Object> variables;
    private String idempotencyKey;
}
