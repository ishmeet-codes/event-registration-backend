package com.registration.management.registration.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.registration.dto.*;
import com.registration.management.registration.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @GetMapping("/api/registrations/{id}")
    @PreAuthorize("hasAuthority('REGISTRATION_VIEW')")
    public ResponseEntity<RegistrationResponseDTO> getRegistrationById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        RegistrationResponseDTO response = registrationService.getRegistrationById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/registrations")
    @PreAuthorize("hasAuthority('REGISTRATION_VIEW')")
    public ResponseEntity<Page<RegistrationResponseDTO>> getRegistrations(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<RegistrationStatus> status,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long createdByStaffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<RegistrationResponseDTO> result = registrationService.getRegistrations(
                search, status, schoolId, eventId, createdByStaffId,
                createdFrom, createdTo, eventDateFrom, eventDateTo,
                page, size, sort, currentUser
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/registrations")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public ResponseEntity<RegistrationResponseDTO> createRegistration(
            @Valid @RequestBody RegistrationCreateRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        RegistrationResponseDTO response = registrationService.createRegistration(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/registrations/bulk")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public ResponseEntity<BulkRegistrationResponseDTO> createBulkRegistrations(
            @Valid @RequestBody BulkRegistrationRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        BulkRegistrationResponseDTO response = registrationService.createBulkRegistrations(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/registrations/{id}")
    @PreAuthorize("hasAuthority('REGISTRATION_UPDATE')")
    public ResponseEntity<RegistrationResponseDTO> updateRegistration(
            @PathVariable("id") Long id,
            @Valid @RequestBody RegistrationUpdateRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        RegistrationResponseDTO response = registrationService.updateRegistration(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/registrations/{id}")
    @PreAuthorize("hasAuthority('REGISTRATION_DELETE')")
    public ResponseEntity<Void> deleteRegistration(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        registrationService.deleteRegistration(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/registrations/{id}/status")
    @PreAuthorize("hasAuthority('REGISTRATION_STATUS_UPDATE')")
    public ResponseEntity<RegistrationResponseDTO> updateRegistrationStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody RegistrationStatusUpdateRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        RegistrationResponseDTO response = registrationService.updateRegistrationStatus(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/registrations/{id}/submit")
    @PreAuthorize("hasAuthority('REGISTRATION_UPDATE')")
    public ResponseEntity<RegistrationResponseDTO> submitRegistration(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) RegistrationRemarksRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        RegistrationResponseDTO response = registrationService.submitRegistration(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/registrations/{id}/approve")
    @PreAuthorize("hasAuthority('REGISTRATION_STATUS_UPDATE')")
    public ResponseEntity<RegistrationResponseDTO> approveRegistration(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        RegistrationResponseDTO response = registrationService.approveRegistration(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/registrations/{id}/reject")
    @PreAuthorize("hasAuthority('REGISTRATION_STATUS_UPDATE')")
    public ResponseEntity<RegistrationResponseDTO> rejectRegistration(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) RegistrationRemarksRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        RegistrationResponseDTO response = registrationService.rejectRegistration(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/registrations/{id}/cancel")
    @PreAuthorize("hasAuthority('REGISTRATION_STATUS_UPDATE')")
    public ResponseEntity<RegistrationResponseDTO> cancelRegistration(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) RegistrationRemarksRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        RegistrationResponseDTO response = registrationService.cancelRegistration(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/registrations/statistics")
    @PreAuthorize("hasAuthority('REGISTRATION_VIEW')")
    public ResponseEntity<RegistrationStatisticsDTO> getRegistrationStatistics(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long schoolId,
            @AuthenticationPrincipal User currentUser
    ) {
        RegistrationStatisticsDTO statistics = registrationService.getRegistrationStatistics(eventId, schoolId, currentUser);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/api/events/{eventId}/registrations")
    @PreAuthorize("hasAuthority('REGISTRATION_VIEW')")
    public ResponseEntity<Page<RegistrationResponseDTO>> getRegistrationsForEvent(
            @PathVariable("eventId") Long eventId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<RegistrationStatus> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<RegistrationResponseDTO> result = registrationService.getRegistrationsForEvent(
                eventId, search, status, page, size, sort, currentUser
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/schools/{schoolId}/registrations")
    @PreAuthorize("hasAuthority('REGISTRATION_VIEW')")
    public ResponseEntity<Page<RegistrationResponseDTO>> getRegistrationsForSchool(
            @PathVariable("schoolId") Long schoolId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<RegistrationStatus> status,
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<RegistrationResponseDTO> result = registrationService.getRegistrationsForSchool(
                schoolId, search, status, eventId, page, size, sort, currentUser
        );
        return ResponseEntity.ok(result);
    }
}
