package com.registration.management.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualSchoolReportDTO {
    private Long schoolId;
    private String schoolCode;
    private String schoolName;
    private String city;
    private long totalRegistrations;
    private long totalParticipants;
    private long totalCheckedIn;
    private double attendanceRate;
    private List<RegistrationReportDTO> registrations;
    private List<ParticipantReportDTO> participants;
}
