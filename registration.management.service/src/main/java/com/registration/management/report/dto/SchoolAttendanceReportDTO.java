package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolAttendanceReportDTO {
    private Long schoolId;
    private String schoolName;
    private long totalRegistered;
    private long totalPresent;
    private double attendanceRate;
}
