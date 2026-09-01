package com.registration.management.school.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.school.dto.schoolDTO;
import com.registration.management.school.service.schoolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_CREATE')")
    public ResponseEntity<schoolDTO> createSchool(
            @Valid @RequestBody schoolDTO request,
            @AuthenticationPrincipal User currentUser) {
        schoolDTO response = schoolService.createSchool(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
