package com.registration.management.notification.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.notification.dto.*;
import com.registration.management.notification.service.AudienceBuilderService;
import com.registration.management.notification.service.EmailCampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-campaigns")
@RequiredArgsConstructor
public class EmailCampaignController {

    private final EmailCampaignService campaignService;
    private final AudienceBuilderService audienceBuilderService;

    @PostMapping
    @PreAuthorize("hasAuthority('EMAIL_CAMPAIGN_CREATE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<CampaignResponseDTO> createCampaign(
            @Valid @RequestBody CampaignRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        CampaignResponseDTO response = campaignService.createCampaign(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMAIL_CAMPAIGN_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Page<CampaignResponseDTO>> getCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CampaignResponseDTO> response = campaignService.getCampaigns(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMAIL_CAMPAIGN_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<CampaignResponseDTO> getCampaignById(@PathVariable("id") Long id) {
        CampaignResponseDTO response = campaignService.getCampaignById(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMAIL_CAMPAIGN_CANCEL') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCampaign(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        campaignService.deleteCampaign(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('EMAIL_CAMPAIGN_CREATE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<CampaignResponseDTO> retryCampaign(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        CampaignResponseDTO response = campaignService.retryCampaign(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/audience/preview")
    @PreAuthorize("hasAuthority('EMAIL_CAMPAIGN_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<AudiencePreviewResponseDTO> previewAudience(
            @RequestBody AudienceCriteriaDTO criteria
    ) {
        AudiencePreviewResponseDTO response = audienceBuilderService.previewAudience(criteria);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('EMAIL_CAMPAIGN_CREATE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> sendTestEmail(
            @PathVariable("id") Long id,
            @RequestParam(required = false) String email,
            @AuthenticationPrincipal User currentUser
    ) {
        campaignService.sendTestEmail(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test-email")
    @PreAuthorize("hasAuthority('EMAIL_CAMPAIGN_CREATE') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> sendDirectTestEmail(
            @RequestParam String email,
            @AuthenticationPrincipal User currentUser
    ) {
        try {
            campaignService.sendDirectTestEmail(email, currentUser);
            return ResponseEntity.ok(java.util.Map.of(
                    "status", "SUCCESS",
                    "message", "Test email successfully delivered to " + email
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                    "status", "ERROR",
                    "error", "Invalid Request",
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "status", "ERROR",
                    "error", "SMTP Delivery Failed",
                    "message", ex.getMessage() != null ? ex.getMessage() : "Unknown mail server error"
            ));
        }
    }

    @GetMapping("/{id}/recipients")
    @PreAuthorize("hasAuthority('EMAIL_CAMPAIGN_VIEW') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<java.util.List<com.registration.management.notification.entity.EmailCampaignRecipient>> getCampaignRecipients(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(campaignService.getCampaignRecipients(id));
    }
}


