package com.registration.management.checkin.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.checkin.dto.BulkCheckinRequestDTO;
import com.registration.management.checkin.dto.BulkCheckinResponseDTO;
import com.registration.management.checkin.dto.CheckinRequestDTO;
import com.registration.management.checkin.dto.CheckinResponseDTO;
import com.registration.management.checkin.dto.CheckinStatusUpdateRequestDTO;
import com.registration.management.checkin.dto.CheckoutRequestDTO;
import com.registration.management.checkin.dto.EventCheckinSummaryDTO;
import com.registration.management.checkin.dto.EventParticipantAttendanceDTO;
import com.registration.management.checkin.enums.CheckinStatus;
import com.registration.management.checkin.service.CheckinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping("/api/checkins")
    @PreAuthorize("hasAuthority('CHECKIN_CREATE') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<CheckinResponseDTO> checkinParticipant(
            @Valid @RequestBody CheckinRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        CheckinResponseDTO response = checkinService.checkinParticipant(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/checkins/{id}")
    @PreAuthorize("hasAuthority('CHECKIN_VIEW') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<CheckinResponseDTO> getCheckinById(@PathVariable("id") Long id) {
        CheckinResponseDTO response = checkinService.getCheckinById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/checkins/{id}")
    @PreAuthorize("hasAuthority('CHECKIN_UPDATE') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<CheckinResponseDTO> correctCheckin(
            @PathVariable("id") Long id,
            @Valid @RequestBody CheckinRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        CheckinResponseDTO response = checkinService.correctCheckin(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/checkins/{id}")
    @PreAuthorize("hasAuthority('CHECKIN_DELETE') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<Void> deleteCheckin(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        checkinService.deleteCheckin(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/checkins")
    @PreAuthorize("hasAuthority('CHECKIN_VIEW') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<Page<CheckinResponseDTO>> getCheckins(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long registrationId,
            @RequestParam(required = false) CheckinStatus status,
            @RequestParam(required = false) Long checkedInBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "checkedInAt,desc") String sort
    ) {
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sortField));
        Page<CheckinResponseDTO> result = checkinService.getCheckins(
                search, eventId, schoolId, registrationId, status, checkedInBy, date, dateFrom, dateTo, pageable
        );
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/api/checkins/{id}/status")
    @PreAuthorize("hasAuthority('CHECKIN_UPDATE') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<CheckinResponseDTO> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody CheckinStatusUpdateRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        CheckinResponseDTO response = checkinService.updateStatus(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/checkins/{id}/checkout")
    @PreAuthorize("hasAuthority('CHECKIN_UPDATE') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<CheckinResponseDTO> checkoutParticipant(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) CheckoutRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        CheckinResponseDTO response = checkinService.checkoutParticipant(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/checkins/participant/{participantId}")
    @PreAuthorize("hasAuthority('CHECKIN_VIEW') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<EventParticipantAttendanceDTO> getParticipantAttendance(@PathVariable("participantId") Long participantId) {
        EventParticipantAttendanceDTO response = checkinService.getParticipantAttendance(participantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/checkins/event/{eventId}")
    @PreAuthorize("hasAuthority('CHECKIN_VIEW') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<CheckinResponseDTO>> getEventCheckins(@PathVariable("eventId") Long eventId) {
        List<CheckinResponseDTO> response = checkinService.getEventCheckins(eventId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/checkins/registration/{registrationId}")
    @PreAuthorize("hasAuthority('CHECKIN_VIEW') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<CheckinResponseDTO>> getRegistrationCheckins(@PathVariable("registrationId") Long registrationId) {
        List<CheckinResponseDTO> response = checkinService.getRegistrationCheckins(registrationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/checkins/school/{schoolId}")
    @PreAuthorize("hasAuthority('CHECKIN_VIEW') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<CheckinResponseDTO>> getSchoolCheckins(@PathVariable("schoolId") Long schoolId) {
        List<CheckinResponseDTO> response = checkinService.getSchoolCheckins(schoolId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/checkins/event/{eventId}/summary")
    @PreAuthorize("hasAuthority('CHECKIN_VIEW_REPORTS') or hasAuthority('CHECKIN_VIEW') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<EventCheckinSummaryDTO> getEventSummary(@PathVariable("eventId") Long eventId) {
        EventCheckinSummaryDTO response = checkinService.getEventSummary(eventId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/checkins/event/{eventId}/participants")
    @PreAuthorize("hasAuthority('CHECKIN_VIEW') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<EventParticipantAttendanceDTO>> getEventParticipantsAttendance(@PathVariable("eventId") Long eventId) {
        List<EventParticipantAttendanceDTO> response = checkinService.getEventParticipantsAttendance(eventId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/checkins/bulk")
    @PreAuthorize("hasAuthority('CHECKIN_BULK') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<BulkCheckinResponseDTO> bulkCheckin(
            @Valid @RequestBody BulkCheckinRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        BulkCheckinResponseDTO response = checkinService.bulkCheckin(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/checkins/event/{eventId}/bulk")
    @PreAuthorize("hasAuthority('CHECKIN_BULK') or hasRole('CHECKIN_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<BulkCheckinResponseDTO> bulkCheckinByEvent(
            @PathVariable("eventId") Long eventId,
            @Valid @RequestBody BulkCheckinRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        BulkCheckinResponseDTO response = checkinService.bulkCheckinByEvent(eventId, request, currentUser);
        return ResponseEntity.ok(response);
    }
}
