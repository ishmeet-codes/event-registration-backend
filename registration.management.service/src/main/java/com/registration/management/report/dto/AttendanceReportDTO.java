package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportDTO {
    private long totalParticipants;
    private long checkedInCount;
    private long checkedOutCount;
    private long notCheckedInCount;
    private double overallAttendanceRate;
}
