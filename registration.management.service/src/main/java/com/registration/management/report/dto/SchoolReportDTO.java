package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolReportDTO {
    private Long schoolId;
    private String schoolCode;
    private String schoolName;
    private String city;
    private long totalRegistrations;
    private long totalParticipants;
    private long totalCheckedIn;
    private double attendanceRate;
}
