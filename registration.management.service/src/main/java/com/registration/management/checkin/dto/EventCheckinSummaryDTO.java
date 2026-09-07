package com.registration.management.checkin.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCheckinSummaryDTO {

    private Long eventId;
    private String eventName;
    private long totalParticipants;
    private long checkedIn;
    private long qrCheckins;
    private long manualCheckins;
    private long checkedOut;
    private long notCheckedIn;
    private long absent;
    private double attendancePercentage;
}
