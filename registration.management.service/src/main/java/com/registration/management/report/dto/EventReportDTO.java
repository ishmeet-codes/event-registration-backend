package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventReportDTO {
    private Long eventId;
    private String eventCode;
    private String title;
    private String categoryName;
    private Integer maxParticipants;
    private long totalRegistered;
    private long totalCheckedIn;
    private double attendanceRate;
}
