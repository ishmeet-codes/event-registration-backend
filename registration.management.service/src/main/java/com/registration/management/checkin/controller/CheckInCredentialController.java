package com.registration.management.checkin.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.checkin.dto.CredentialResponseDTO;
import com.registration.management.checkin.service.CheckInCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checkin/credential")
@RequiredArgsConstructor
public class CheckInCredentialController {

    private final CheckInCredentialService credentialService;

    @GetMapping
    @PreAuthorize("hasAuthority('CHECKIN_CREDENTIAL_VIEW') or isAuthenticated()")
    public ResponseEntity<List<CredentialResponseDTO>> getMyCredentials(
            @AuthenticationPrincipal User currentUser
    ) {
        List<CredentialResponseDTO> credentials = credentialService.getMyActiveCredentials(currentUser);
        return ResponseEntity.ok(credentials);
    }

    @GetMapping("/registration/{registrationId}")
    @PreAuthorize("hasAuthority('CHECKIN_CREDENTIAL_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM')")
    public ResponseEntity<List<CredentialResponseDTO>> getRegistrationCredentials(
            @PathVariable("registrationId") Long registrationId
    ) {
        List<CredentialResponseDTO> credentials = credentialService.getCredentialsByRegistrationId(registrationId);
        return ResponseEntity.ok(credentials);
    }

    @PostMapping("/{id}/regenerate")
    @PreAuthorize("hasAuthority('CHECKIN_CREDENTIAL_REGENERATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER') or hasRole('CHECKIN_TEAM') or isAuthenticated()")
    public ResponseEntity<CredentialResponseDTO> regenerateCredential(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        CredentialResponseDTO updated = credentialService.regenerateCredential(id, currentUser);
        return ResponseEntity.ok(updated);
    }
}
