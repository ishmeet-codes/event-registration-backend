package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationReportDTO {
    private long totalCategories;
    private Map<String, Long> categoryBreakdown;
    private String topCategory;
}
