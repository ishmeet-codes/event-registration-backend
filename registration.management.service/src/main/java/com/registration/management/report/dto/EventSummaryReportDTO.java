package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryReportDTO {
    private Long eventId;
    private String title;
    private String eventCode;
    private long registeredCount;
    private long checkedInCount;
}
