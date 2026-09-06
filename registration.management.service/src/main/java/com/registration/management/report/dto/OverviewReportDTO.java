package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewReportDTO {
    private long totalRegistrations;
    private long confirmedRegistrations;
    private long pendingRegistrations;
    private double totalRevenue;
    private long totalParticipants;
    private double overallAttendanceRate;
    private List<DailyMetricDTO> dailyRegistrations;
    private List<DailyMetricDTO> dailyAttendance;
}
