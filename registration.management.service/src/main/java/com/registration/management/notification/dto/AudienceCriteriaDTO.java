package com.registration.management.notification.dto;

import com.registration.management.notification.enums.RecipientType;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudienceCriteriaDTO {
    private RecipientType recipientType;
    private Long eventId;
    private Long schoolId;
    private String staffRole;
    private String registrationStatus;
    private String attendanceStatus;
    private List<String> selectedEmails;
    private List<String> excludedEmails;
}

