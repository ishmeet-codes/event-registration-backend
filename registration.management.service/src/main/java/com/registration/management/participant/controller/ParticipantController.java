package com.registration.management.participant.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.enums.Gender;
import com.registration.management.participant.dto.*;
import com.registration.management.participant.service.ParticipantService;
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

@RestController
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @PostMapping("/api/participants")
    @PreAuthorize("hasAuthority('PARTICIPANT_CREATE')")
    public ResponseEntity<ParticipantResponseDTO> createParticipant(
            @Valid @RequestBody ParticipantCreateRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        ParticipantResponseDTO response = participantService.createParticipant(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/participants/{id}")
    @PreAuthorize("hasAuthority('PARTICIPANT_VIEW')")
    public ResponseEntity<ParticipantResponseDTO> getParticipantById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        ParticipantResponseDTO response = participantService.getParticipantById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/participants")
    @PreAuthorize("hasAuthority('PARTICIPANT_VIEW')")
    public ResponseEntity<Page<ParticipantResponseDTO>> getParticipants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long registrationId,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dobFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dobTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<ParticipantResponseDTO> result = participantService.getParticipants(
                search, registrationId, schoolId, eventId, gender, className, dobFrom, dobTo, page, size, sort, currentUser
        );
        return ResponseEntity.ok(result);
    }

    @PutMapping("/api/participants/{id}")
    @PreAuthorize("hasAuthority('PARTICIPANT_UPDATE')")
    public ResponseEntity<ParticipantResponseDTO> updateParticipant(
            @PathVariable("id") Long id,
            @Valid @RequestBody ParticipantUpdateRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        ParticipantResponseDTO response = participantService.updateParticipant(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/participants/{id}")
    @PreAuthorize("hasAuthority('PARTICIPANT_DELETE')")
    public ResponseEntity<Void> deleteParticipant(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        participantService.deleteParticipant(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/registrations/{registrationId}/participants")
    @PreAuthorize("hasAuthority('PARTICIPANT_VIEW')")
    public ResponseEntity<Page<ParticipantResponseDTO>> getParticipantsByRegistration(
            @PathVariable("registrationId") Long registrationId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) String className,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<ParticipantResponseDTO> result = participantService.getParticipantsByRegistration(
                registrationId, search, gender, className, page, size, sort, currentUser
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/registrations/{registrationId}/participants/count")
    @PreAuthorize("hasAuthority('PARTICIPANT_VIEW')")
    public ResponseEntity<ParticipantCountResponseDTO> getParticipantCount(
            @PathVariable("registrationId") Long registrationId,
            @AuthenticationPrincipal User currentUser
    ) {
        ParticipantCountResponseDTO response = participantService.getParticipantCount(registrationId, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/schools/{schoolId}/participants")
    @PreAuthorize("hasAuthority('PARTICIPANT_VIEW')")
    public ResponseEntity<Page<ParticipantResponseDTO>> getParticipantsBySchool(
            @PathVariable("schoolId") Long schoolId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) String className,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<ParticipantResponseDTO> result = participantService.getParticipantsBySchool(
                schoolId, search, eventId, gender, className, page, size, sort, currentUser
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/events/{eventId}/participants")
    @PreAuthorize("hasAuthority('PARTICIPANT_VIEW')")
    public ResponseEntity<Page<ParticipantResponseDTO>> getParticipantsByEvent(
            @PathVariable("eventId") Long eventId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) String className,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<ParticipantResponseDTO> result = participantService.getParticipantsByEvent(
                eventId, search, schoolId, gender, className, page, size, sort, currentUser
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/participants/{id}/eligibility")
    @PreAuthorize("hasAuthority('PARTICIPANT_VIEW')")
    public ResponseEntity<ParticipantEligibilityResponseDTO> checkEligibility(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        ParticipantEligibilityResponseDTO response = participantService.checkEligibility(id, currentUser);
        return ResponseEntity.ok(response);
    }
}
