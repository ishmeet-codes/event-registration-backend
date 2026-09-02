package com.registration.management.school.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.school.dto.SchoolStatusRequestDTO;
import com.registration.management.school.dto.schoolDTO;
import com.registration.management.school.service.schoolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schools")
public class schoolController {

    @Autowired
    private schoolService schoolService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_VIEW')")
    public ResponseEntity<Page<schoolDTO>> getSchools(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String board,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "schoolName,asc") String sort) {
        Page<schoolDTO> result = schoolService.getSchools(search, city, district, state, board, active, page, size, sort);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{schoolId}")
    @PreAuthorize("hasAuthority('SCHOOL_VIEW')")
    public ResponseEntity<schoolDTO> getSchoolById(
            @PathVariable("schoolId") Long schoolId,
            @RequestParam(required = false) Boolean includeSummary,
            @RequestParam(required = false) Boolean summary) {
        boolean withSummary = Boolean.TRUE.equals(includeSummary) || Boolean.TRUE.equals(summary);
        schoolDTO result = schoolService.getSchoolById(schoolId, withSummary);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_CREATE')")
    public ResponseEntity<schoolDTO> createSchool(
            @Valid @RequestBody schoolDTO request,
            @AuthenticationPrincipal User currentUser) {
        schoolDTO response = schoolService.createSchool(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{schoolId}")
    @PreAuthorize("hasAuthority('SCHOOL_UPDATE')")
    public ResponseEntity<schoolDTO> updateSchool(
            @PathVariable("schoolId") Long schoolId,
            @Valid @RequestBody schoolDTO request,
            @AuthenticationPrincipal User currentUser) {
        schoolDTO response = schoolService.updateSchool(schoolId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{schoolId}/status")
    @PreAuthorize("hasAuthority('SCHOOL_STATUS_UPDATE')")
    public ResponseEntity<schoolDTO> updateSchoolStatus(
            @PathVariable("schoolId") Long schoolId,
            @Valid @RequestBody SchoolStatusRequestDTO request,
            @AuthenticationPrincipal User currentUser) {
        schoolDTO response = schoolService.updateSchoolStatus(schoolId, request.getActive(), currentUser);
        return ResponseEntity.ok(response);
    }
}
