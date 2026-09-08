package com.registration.management.notification.mapper;

import com.registration.management.notification.dto.*;
import com.registration.management.notification.entity.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final ModelMapper modelMapper;

    public NotificationResponseDTO toNotificationResponseDTO(Notification n) {
        if (n == null) return null;

        NotificationResponseDTO dto = NotificationResponseDTO.builder()
                .id(n.getId())
                .type(n.getType())
                .channel(n.getChannel())
                .title(n.getTitle())
                .message(n.getMessage())
                .status(n.getStatus())
                .read(n.isRead())
                .retryCount(n.getRetryCount())
                .scheduledAt(n.getScheduledAt())
                .sentAt(n.getSentAt())
                .deliveredAt(n.getDeliveredAt())
                .readAt(n.getReadAt())
                .failureReason(n.getFailureReason())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();

        if (n.getRecipient() != null) {
            dto.setRecipientUserId(n.getRecipient().getId());
            dto.setRecipientName(n.getRecipient().getFullName());
            dto.setRecipientEmail(n.getRecipient().getEmail());
        }

        if (n.getReferenceType() != null || n.getReferenceId() != null) {
            dto.setReference(ReferenceDTO.builder()
                    .type(n.getReferenceType())
                    .id(n.getReferenceId())
                    .build());
        }

        return dto;
    }

    public NotificationTemplateResponseDTO toTemplateResponseDTO(NotificationTemplate t) {
        if (t == null) return null;
        return modelMapper.map(t, NotificationTemplateResponseDTO.class);
    }

    public NotificationPreferenceDTO toPreferenceDTO(NotificationPreference p) {
        if (p == null) return null;
        NotificationPreferenceDTO dto = modelMapper.map(p, NotificationPreferenceDTO.class);
        if (p.getUser() != null) {
            dto.setUserId(p.getUser().getId());
        }
        return dto;
    }

    public NotificationLogDTO toLogDTO(NotificationLog l) {
        if (l == null) return null;
        NotificationLogDTO dto = modelMapper.map(l, NotificationLogDTO.class);
        if (l.getNotification() != null) {
            dto.setNotificationId(l.getNotification().getId());
        }
        return dto;
    }
}
