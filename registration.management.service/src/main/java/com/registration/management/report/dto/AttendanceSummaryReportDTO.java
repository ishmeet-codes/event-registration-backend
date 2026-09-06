package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryReportDTO {
    private long totalExpected;
    private long totalPresent;
    private long totalAbsent;
    private double attendancePercentage;
}
