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
public class ParticipantReportDTO {
    private Long id;
    private String fullName;
    private String gender;
    private String className;
    private Long schoolId;
    private String schoolName;
    private String registrationNumber;
    private List<String> registeredEvents;
    private String checkinStatus;
}
