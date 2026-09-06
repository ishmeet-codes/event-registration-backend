package com.registration.management.checkin.dto;

import com.registration.management.checkin.enums.CheckinStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResponseDTO {

    private Long id;
    private ParticipantSummary participant;
    private SchoolSummary school;
    private EventSummary event;
    private RegistrationSummary registration;
    private CheckinStatus status;
    private LocalDateTime checkedInAt;
    private UserSummary checkedInBy;
    private LocalDateTime checkedOutAt;
    private UserSummary checkedOutBy;
    private String remarks;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantSummary {
        private Long id;
        private String name;
        private String className;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchoolSummary {
        private Long id;
        private String name;
        private String code;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventSummary {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrationSummary {
        private Long id;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String name;
    }
}
