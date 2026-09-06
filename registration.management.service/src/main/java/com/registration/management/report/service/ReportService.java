package com.registration.management.report.service;

import com.registration.management.auth.entities.User;
import com.registration.management.report.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    DashboardReportDTO getDashboardReport(User currentUser);

    OverviewReportDTO getOverviewReport(LocalDate startDate, LocalDate endDate, User currentUser);

    List<RegistrationReportDTO> getRegistrationReports(String status, Long schoolId, Long eventId, LocalDate startDate, LocalDate endDate, User currentUser);

    RegistrationSummaryDTO getRegistrationSummary(User currentUser);

    RegistrationReportDTO getRegistrationReportById(Long registrationId, User currentUser);

    List<ParticipantReportDTO> getParticipantReports(String gender, Long schoolId, Long eventId, String search, User currentUser);

    ParticipantSummaryDTO getParticipantSummary(User currentUser);

    List<EventReportDTO> getEventReports(User currentUser);

    IndividualEventReportDTO getEventReportById(Long eventId, User currentUser);

    EventSummaryReportDTO getEventSummary(User currentUser);

    List<SchoolReportDTO> getSchoolReports(User currentUser);

    IndividualSchoolReportDTO getSchoolReportById(Long schoolId, User currentUser);

    AttendanceReportDTO getAttendanceReport(User currentUser);

    AttendanceSummaryReportDTO getAttendanceSummary(User currentUser);

    EventAttendanceReportDTO getEventAttendanceReport(Long eventId, User currentUser);

    SchoolAttendanceReportDTO getSchoolAttendanceReport(Long schoolId, User currentUser);

    ParticipationReportDTO getParticipationReport(User currentUser);

    byte[] exportReport(String type, String format, Long schoolId, Long eventId, User currentUser);
}
