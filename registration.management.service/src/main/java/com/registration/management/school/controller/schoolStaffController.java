package com.registration.management.school.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.enums.StaffRole;
import com.registration.management.school.dto.StaffStatusRequestDTO;
import com.registration.management.school.dto.schoolStaffDTO;
import com.registration.management.school.service.schoolStaffService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schools/{schoolId}/staff")
public class schoolStaffController {

    @Autowired
    private schoolStaffService schoolStaffService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_STAFF_CREATE')")
    public ResponseEntity<schoolStaffDTO> createStaff(
            @PathVariable("schoolId") Long schoolId,
            @Valid @RequestBody schoolStaffDTO request,
            @AuthenticationPrincipal User currentUser) {
        schoolStaffDTO response = schoolStaffService.createStaff(schoolId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_STAFF_VIEW')")
    public ResponseEntity<Page<schoolStaffDTO>> getStaffList(
            @PathVariable("schoolId") Long schoolId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StaffRole staffRole,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort) {
        Page<schoolStaffDTO> result = schoolStaffService.getStaffList(schoolId, search, staffRole, active, page, size, sort);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{staffId}")
    @PreAuthorize("hasAuthority('SCHOOL_STAFF_VIEW')")
    public ResponseEntity<schoolStaffDTO> getStaffById(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("staffId") Long staffId) {
        schoolStaffDTO response = schoolStaffService.getStaffById(schoolId, staffId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{staffId}")
    @PreAuthorize("hasAuthority('SCHOOL_STAFF_UPDATE')")
    public ResponseEntity<schoolStaffDTO> updateStaff(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("staffId") Long staffId,
            @Valid @RequestBody schoolStaffDTO request,
            @AuthenticationPrincipal User currentUser) {
        schoolStaffDTO response = schoolStaffService.updateStaff(schoolId, staffId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{staffId}/status")
    @PreAuthorize("hasAuthority('SCHOOL_STAFF_STATUS_UPDATE') or hasAuthority('SCHOOL_STAFF_UPDATE')")
    public ResponseEntity<schoolStaffDTO> updateStaffStatus(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("staffId") Long staffId,
            @Valid @RequestBody StaffStatusRequestDTO request,
            @AuthenticationPrincipal User currentUser) {
        schoolStaffDTO response = schoolStaffService.updateStaffStatus(schoolId, staffId, request.getActive(), currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{staffId}")
    @PreAuthorize("hasAuthority('SCHOOL_STAFF_DELETE')")
    public ResponseEntity<Void> deleteStaff(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("staffId") Long staffId,
            @AuthenticationPrincipal User currentUser) {
        schoolStaffService.deleteStaff(schoolId, staffId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
