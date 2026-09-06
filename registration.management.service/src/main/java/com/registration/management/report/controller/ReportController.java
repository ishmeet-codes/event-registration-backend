package com.registration.management.report.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.report.dto.*;
import com.registration.management.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<DashboardReportDTO> getDashboardReport(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getDashboardReport(currentUser));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<OverviewReportDTO> getOverviewReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(reportService.getOverviewReport(startDate, endDate, currentUser));
    }

    @GetMapping("/registrations")
    @PreAuthorize("hasAuthority('REPORT_VIEW_DETAILED') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<List<RegistrationReportDTO>> getRegistrationReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(reportService.getRegistrationReports(status, schoolId, eventId, startDate, endDate, currentUser));
    }

    @GetMapping("/registrations/summary")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<RegistrationSummaryDTO> getRegistrationSummary(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getRegistrationSummary(currentUser));
    }

    @GetMapping("/registrations/{registrationId}")
    @PreAuthorize("hasAuthority('REPORT_VIEW_DETAILED') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<RegistrationReportDTO> getRegistrationReportById(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(reportService.getRegistrationReportById(registrationId, currentUser));
    }

    @GetMapping("/participants")
    @PreAuthorize("hasAuthority('REPORT_VIEW_DETAILED') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<List<ParticipantReportDTO>> getParticipantReports(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(reportService.getParticipantReports(gender, schoolId, eventId, search, currentUser));
    }

    @GetMapping("/participants/summary")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<ParticipantSummaryDTO> getParticipantSummary(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getParticipantSummary(currentUser));
    }

    @GetMapping("/events")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<List<EventReportDTO>> getEventReports(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getEventReports(currentUser));
    }

    @GetMapping("/events/{eventId}")
    @PreAuthorize("hasAuthority('REPORT_VIEW_DETAILED') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<IndividualEventReportDTO> getEventReportById(
            @PathVariable Long eventId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(reportService.getEventReportById(eventId, currentUser));
    }

    @GetMapping("/events/summary")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<EventSummaryReportDTO> getEventSummary(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getEventSummary(currentUser));
    }

    @GetMapping("/schools")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<List<SchoolReportDTO>> getSchoolReports(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getSchoolReports(currentUser));
    }

    @GetMapping("/schools/{schoolId}")
    @PreAuthorize("hasAuthority('REPORT_VIEW_DETAILED') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<IndividualSchoolReportDTO> getSchoolReportById(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(reportService.getSchoolReportById(schoolId, currentUser));
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAuthority('REPORT_VIEW_ATTENDANCE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<AttendanceReportDTO> getAttendanceReport(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getAttendanceReport(currentUser));
    }

    @GetMapping("/attendance/summary")
    @PreAuthorize("hasAuthority('REPORT_VIEW_ATTENDANCE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<AttendanceSummaryReportDTO> getAttendanceSummary(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getAttendanceSummary(currentUser));
    }

    @GetMapping("/attendance/events/{eventId}")
    @PreAuthorize("hasAuthority('REPORT_VIEW_ATTENDANCE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<EventAttendanceReportDTO> getEventAttendanceReport(
            @PathVariable Long eventId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(reportService.getEventAttendanceReport(eventId, currentUser));
    }

    @GetMapping("/attendance/schools/{schoolId}")
    @PreAuthorize("hasAuthority('REPORT_VIEW_ATTENDANCE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<SchoolAttendanceReportDTO> getSchoolAttendanceReport(
            @PathVariable Long schoolId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(reportService.getSchoolAttendanceReport(schoolId, currentUser));
    }

    @GetMapping("/participation")
    @PreAuthorize("hasAuthority('REPORT_VIEW_ANALYTICS') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM')")
    public ResponseEntity<ParticipationReportDTO> getParticipationReport(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reportService.getParticipationReport(currentUser));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('REPORT_EXPORT') or hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or hasRole('SCHOOL_STAFF') or hasRole('LOGIN_TEACHER') or hasRole('ACCOMPANYING_TEACHER')")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(defaultValue = "REGISTRATION") String type,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal User currentUser
    ) {
        byte[] csvData = reportService.exportReport(type, format, schoolId, eventId, currentUser);
        String filename = String.format("report_%s_%s.csv", type.toLowerCase(), LocalDate.now().toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
