package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationReportDTO {
    private Long id;
    private String registrationNumber;
    private Long schoolId;
    private String schoolName;
    private String schoolCode;
    private String status;
    private int participantCount;
    private Double totalFee;
    private Double paidAmount;
    private String paymentStatus;
    private LocalDateTime createdAt;
}
