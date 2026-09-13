package com.registration.management.refreshment.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private Long planId;
    private String planName;
    private Long totalEligible;
    private Long totalPresent;
    private Long totalDistributed;
    private Long totalPending;
    private Double overallDistributionRate;
    private List<CategorySummaryDTO> categoryWise;
    private List<EventSummaryDTO> eventWise;
}
