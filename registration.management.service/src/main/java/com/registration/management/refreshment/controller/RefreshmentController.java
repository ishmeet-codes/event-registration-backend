package com.registration.management.refreshment.controller;

import com.registration.management.auth.entities.User;
import com.registration.management.refreshment.dto.*;
import com.registration.management.refreshment.enums.RecipientCategory;
import com.registration.management.refreshment.service.RefreshmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RefreshmentController {

    private final RefreshmentService refreshmentService;

    // =========================================================================
    // 1. REFRESHMENT PLANS
    // =========================================================================

    @PostMapping("/api/refreshment/plans")
    @PreAuthorize("hasAuthority('REFRESHMENT_CREATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<RefreshmentPlanResponseDTO> createPlan(
            @Valid @RequestBody RefreshmentPlanRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        RefreshmentPlanResponseDTO response = refreshmentService.createPlan(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/refreshment/plans")
    @PreAuthorize("hasAuthority('REFRESHMENT_VIEW') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<RefreshmentPlanResponseDTO>> getAllPlans() {
        return ResponseEntity.ok(refreshmentService.getAllPlans());
    }

    @GetMapping("/api/refreshment/plans/{id}")
    @PreAuthorize("hasAuthority('REFRESHMENT_VIEW') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<RefreshmentPlanResponseDTO> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(refreshmentService.getPlanById(id));
    }

    @PutMapping("/api/refreshment/plans/{id}")
    @PreAuthorize("hasAuthority('REFRESHMENT_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<RefreshmentPlanResponseDTO> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody RefreshmentPlanRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(refreshmentService.updatePlan(id, request, currentUser));
    }

    @DeleteMapping("/api/refreshment/plans/{id}")
    @PreAuthorize("hasAuthority('REFRESHMENT_DELETE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<Void> deletePlan(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        refreshmentService.deletePlan(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // 2. REFRESHMENT SESSIONS
    // =========================================================================

    @PostMapping("/api/refreshment/sessions")
    @PreAuthorize("hasAuthority('REFRESHMENT_SESSION_MANAGE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<RefreshmentSessionResponseDTO> createSession(
            @Valid @RequestBody RefreshmentSessionRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        RefreshmentSessionResponseDTO response = refreshmentService.createSession(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/refreshment/sessions/{id}")
    @PreAuthorize("hasAuthority('REFRESHMENT_VIEW') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<RefreshmentSessionResponseDTO> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(refreshmentService.getSessionById(id));
    }

    @GetMapping("/api/refreshment/plans/{planId}/sessions")
    @PreAuthorize("hasAuthority('REFRESHMENT_VIEW') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<RefreshmentSessionResponseDTO>> getSessionsByPlan(@PathVariable Long planId) {
        return ResponseEntity.ok(refreshmentService.getSessionsByPlan(planId));
    }

    @PutMapping("/api/refreshment/sessions/{id}")
    @PreAuthorize("hasAuthority('REFRESHMENT_SESSION_MANAGE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<RefreshmentSessionResponseDTO> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody RefreshmentSessionRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(refreshmentService.updateSession(id, request, currentUser));
    }

    @DeleteMapping("/api/refreshment/sessions/{id}")
    @PreAuthorize("hasAuthority('REFRESHMENT_SESSION_MANAGE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<Void> deleteSession(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        refreshmentService.deleteSession(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // 3. REFRESHMENT TEAMS & ASSIGNMENTS
    // =========================================================================

    @PostMapping("/api/refreshment/teams")
    @PreAuthorize("hasAuthority('REFRESHMENT_TEAM_MANAGE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<RefreshmentTeamResponseDTO> createTeam(
            @Valid @RequestBody RefreshmentTeamRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        RefreshmentTeamResponseDTO response = refreshmentService.createTeam(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/refreshment/teams")
    @PreAuthorize("hasAuthority('REFRESHMENT_VIEW') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<RefreshmentTeamResponseDTO>> getAllTeams() {
        return ResponseEntity.ok(refreshmentService.getAllTeams());
    }

    @GetMapping("/api/refreshment/teams/{id}")
    @PreAuthorize("hasAuthority('REFRESHMENT_VIEW') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<RefreshmentTeamResponseDTO> getTeamById(@PathVariable Long id) {
        return ResponseEntity.ok(refreshmentService.getTeamById(id));
    }

    @PutMapping("/api/refreshment/teams/{id}")
    @PreAuthorize("hasAuthority('REFRESHMENT_TEAM_MANAGE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<RefreshmentTeamResponseDTO> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody RefreshmentTeamRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(refreshmentService.updateTeam(id, request, currentUser));
    }

    @DeleteMapping("/api/refreshment/teams/{id}")
    @PreAuthorize("hasAuthority('REFRESHMENT_TEAM_MANAGE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        refreshmentService.deleteTeam(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/refreshment/assignments")
    @PreAuthorize("hasAuthority('REFRESHMENT_ASSIGNMENT_MANAGE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<Void> assignTeamToSession(
            @Valid @RequestBody RefreshmentAssignmentRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        refreshmentService.assignTeamToSession(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/refreshment/assignments/{sessionId}/{teamId}")
    @PreAuthorize("hasAuthority('REFRESHMENT_ASSIGNMENT_MANAGE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<Void> removeTeamAssignment(
            @PathVariable Long sessionId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal User currentUser
    ) {
        refreshmentService.removeTeamAssignment(sessionId, teamId, currentUser);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // 4. REFRESHMENT TEAM PORTAL (FIELD OPERATIONS)
    // =========================================================================

    @GetMapping("/api/refreshment/team/my-sessions")
    @PreAuthorize("hasAuthority('REFRESHMENT_VIEW') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<TeamSessionSummaryDTO>> getMyAssignedSessions(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(refreshmentService.getMyAssignedSessions(currentUser));
    }

    @GetMapping("/api/refreshment/sessions/{sessionId}/recipients")
    @PreAuthorize("hasAuthority('REFRESHMENT_DISTRIBUTION_VIEW') or hasAuthority('REFRESHMENT_VIEW') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<RecipientDistributionDTO>> getSessionRecipients(
            @PathVariable Long sessionId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(refreshmentService.getSessionRecipients(sessionId, status, search));
    }

    // =========================================================================
    // 5. DISTRIBUTION EXECUTION
    // =========================================================================

    @PostMapping("/api/refreshment/distributions/mark-given")
    @PreAuthorize("hasAuthority('REFRESHMENT_DISTRIBUTE') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<DistributionResponseDTO> markGiven(
            @Valid @RequestBody MarkGivenRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        DistributionResponseDTO response = refreshmentService.markGiven(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/refreshment/distributions/mark-given-bulk")
    @PreAuthorize("hasAuthority('REFRESHMENT_DISTRIBUTE') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<DistributionResponseDTO>> markGivenBulk(
            @Valid @RequestBody MarkGivenBulkRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        List<DistributionResponseDTO> response = refreshmentService.markGivenBulk(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/refreshment/sessions/{sessionId}/guest-count/increment")
    @PreAuthorize("hasAuthority('REFRESHMENT_DISTRIBUTE') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<RefreshmentSessionResponseDTO> incrementGuestCount(
            @PathVariable Long sessionId,
            @Valid @RequestBody GuestCountIncrementDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        RefreshmentSessionResponseDTO response = refreshmentService.incrementGuestCount(sessionId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 6. CORRECTION & AUDIT
    // =========================================================================

    @PostMapping("/api/refreshment/distributions/{distributionId}/correct")
    @PreAuthorize("hasAuthority('REFRESHMENT_DISTRIBUTION_CORRECT') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<DistributionResponseDTO> correctDistribution(
            @PathVariable Long distributionId,
            @Valid @RequestBody CorrectionRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        DistributionResponseDTO response = refreshmentService.correctDistribution(distributionId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/refreshment/distributions/{distributionId}/history")
    @PreAuthorize("hasAuthority('REFRESHMENT_DISTRIBUTION_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<DistributionResponseDTO>> getDistributionHistory(@PathVariable Long distributionId) {
        return ResponseEntity.ok(refreshmentService.getDistributionHistory(distributionId));
    }

    // =========================================================================
    // 7. UNIVERSAL RECIPIENT SEARCH & DIRECTORY
    // =========================================================================

    @GetMapping("/api/refreshment/recipients/lookup")
    @PreAuthorize("hasAuthority('REFRESHMENT_VIEW') or hasRole('REFRESHMENT_TEAM') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<RecipientLookupDTO>> lookupRecipient(
            @RequestParam String query,
            @RequestParam(required = false) Long planId
    ) {
        return ResponseEntity.ok(refreshmentService.lookupRecipient(query, planId));
    }

    @GetMapping("/api/refreshment/recipients")
    @PreAuthorize("hasAuthority('REFRESHMENT_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<RecipientDistributionDTO>> getGlobalRecipients(
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) RecipientCategory category,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(refreshmentService.getGlobalRecipients(planId, eventId, category, status, schoolId, search));
    }

    // =========================================================================
    // 8. GUEST RECORDS
    // =========================================================================

    @PostMapping("/api/refreshment/guests")
    @PreAuthorize("hasAuthority('REFRESHMENT_SESSION_MANAGE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<GuestRecordResponseDTO> createGuest(
            @Valid @RequestBody GuestRecordRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        GuestRecordResponseDTO response = refreshmentService.createGuestRecord(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/refreshment/plans/{planId}/guests")
    @PreAuthorize("hasAuthority('REFRESHMENT_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<List<GuestRecordResponseDTO>> getGuestsByPlan(@PathVariable Long planId) {
        return ResponseEntity.ok(refreshmentService.getGuestsByPlan(planId));
    }

    @DeleteMapping("/api/refreshment/guests/{id}")
    @PreAuthorize("hasAuthority('REFRESHMENT_SESSION_MANAGE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<Void> deleteGuest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        refreshmentService.deleteGuestRecord(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // 9. OC DASHBOARD SUMMARY
    // =========================================================================

    @GetMapping("/api/refreshment/dashboard/summary")
    @PreAuthorize("hasAuthority('REFRESHMENT_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('EVENT_MANAGER')")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary(
            @RequestParam(required = false) Long planId
    ) {
        return ResponseEntity.ok(refreshmentService.getDashboardSummary(planId));
    }
}
