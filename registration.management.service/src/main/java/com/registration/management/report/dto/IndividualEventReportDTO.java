package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualEventReportDTO {
    private Long eventId;
    private String eventCode;
    private String title;
    private String categoryName;
    private LocalDateTime eventDate;
    private String venue;
    private long totalRegistered;
    private long totalCheckedIn;
    private double attendanceRate;
    private long participatingSchoolsCount;
    private List<ParticipantReportDTO> participants;
}
