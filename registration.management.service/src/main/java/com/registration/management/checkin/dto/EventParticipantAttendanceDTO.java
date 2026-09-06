package com.registration.management.checkin.dto;

import com.registration.management.checkin.enums.CheckinStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipantAttendanceDTO {

    private Long participantId;
    private String participantName;
    private String className;
    private Long schoolId;
    private String schoolName;
    private String schoolCode;
    private Long registrationId;
    private Long checkinId;
    private CheckinStatus status;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
    private Long checkedInById;
    private String checkedInBy;
    private Long checkedOutById;
    private String checkedOutBy;
    private String remarks;
}
