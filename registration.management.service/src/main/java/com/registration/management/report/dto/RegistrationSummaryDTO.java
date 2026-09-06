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
public class RegistrationSummaryDTO {
    private long totalRegistrations;
    private Map<String, Long> byStatus;
    private Map<String, Long> byPaymentStatus;
    private Double totalFeesCollected;
    private Double totalFeesPending;
}
