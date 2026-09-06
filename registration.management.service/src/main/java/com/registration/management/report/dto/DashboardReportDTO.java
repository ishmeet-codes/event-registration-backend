package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardReportDTO {
    private long totalRegistrations;
    private long totalParticipants;
    private long totalCheckins;
    private long totalSchools;
    private long totalEvents;
    private Map<String, Long> registrationStatusBreakdown;
    private Map<String, Long> checkinStatusBreakdown;
    private List<EventSummaryReportDTO> topEvents;
    private List<SchoolSummaryReportDTO> topSchools;
}
