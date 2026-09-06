package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSummaryReportDTO {
    private Long schoolId;
    private String schoolName;
    private String schoolCode;
    private long participantCount;
}
