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
public class ParticipantSummaryDTO {
    private long totalParticipants;
    private Map<String, Long> byGender;
    private Map<String, Long> byClass;
}
