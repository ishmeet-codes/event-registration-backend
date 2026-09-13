package com.registration.management.refreshment.dto;

import com.registration.management.refreshment.enums.RecipientCategory;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientLookupDTO {
    private Long recipientId;
    private RecipientCategory recipientCategory;
    private String name;
    private String schoolName;
    private Long eventId;
    private String eventName;
    private String attendanceStatus;
    private List<SessionStatusDTO> sessions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionStatusDTO {
        private Long sessionId;
        private String sessionName;
        private String refreshmentStatus; // GIVEN, PENDING, NOT_ELIGIBLE
        private Long distributionId;
        private String distributedByName;
        private String distributedAt;
    }
}
