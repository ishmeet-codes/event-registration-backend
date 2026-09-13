package com.registration.management.refreshment.service;

import com.registration.management.auth.entities.User;
import com.registration.management.refreshment.dto.*;
import com.registration.management.refreshment.enums.RecipientCategory;

import java.util.List;

public interface RefreshmentService {

    // Plan Management
    RefreshmentPlanResponseDTO createPlan(RefreshmentPlanRequestDTO request, User actor);
    RefreshmentPlanResponseDTO getPlanById(Long id);
    List<RefreshmentPlanResponseDTO> getAllPlans();
    RefreshmentPlanResponseDTO updatePlan(Long id, RefreshmentPlanRequestDTO request, User actor);
    void deletePlan(Long id, User actor);

    // Session Management
    RefreshmentSessionResponseDTO createSession(RefreshmentSessionRequestDTO request, User actor);
    RefreshmentSessionResponseDTO getSessionById(Long id);
    List<RefreshmentSessionResponseDTO> getSessionsByPlan(Long planId);
    RefreshmentSessionResponseDTO updateSession(Long id, RefreshmentSessionRequestDTO request, User actor);
    void deleteSession(Long id, User actor);

    // Team Management & Assignments
    RefreshmentTeamResponseDTO createTeam(RefreshmentTeamRequestDTO request, User actor);
    RefreshmentTeamResponseDTO getTeamById(Long id);
    List<RefreshmentTeamResponseDTO> getAllTeams();
    RefreshmentTeamResponseDTO updateTeam(Long id, RefreshmentTeamRequestDTO request, User actor);
    void deleteTeam(Long id, User actor);
    void assignTeamToSession(RefreshmentAssignmentRequestDTO request, User actor);
    void removeTeamAssignment(Long sessionId, Long teamId, User actor);

    // Team Portal (Field Operations)
    List<TeamSessionSummaryDTO> getMyAssignedSessions(User actor);
    List<RecipientDistributionDTO> getSessionRecipients(Long sessionId, String status, String search);

    // Distribution Execution
    DistributionResponseDTO markGiven(MarkGivenRequestDTO request, User actor);
    List<DistributionResponseDTO> markGivenBulk(MarkGivenBulkRequestDTO request, User actor);
    RefreshmentSessionResponseDTO incrementGuestCount(Long sessionId, GuestCountIncrementDTO request, User actor);

    // Correction & Audit
    DistributionResponseDTO correctDistribution(Long distributionId, CorrectionRequestDTO request, User actor);
    List<DistributionResponseDTO> getDistributionHistory(Long distributionId);

    // Search & Universal Recipient Lookup
    List<RecipientLookupDTO> lookupRecipient(String query, Long planId);
    List<RecipientDistributionDTO> getGlobalRecipients(Long planId, Long eventId, RecipientCategory category, String status, Long schoolId, String search);

    // Guest Records (Individual Tracking)
    GuestRecordResponseDTO createGuestRecord(GuestRecordRequestDTO request, User actor);
    List<GuestRecordResponseDTO> getGuestsByPlan(Long planId);
    void deleteGuestRecord(Long id, User actor);

    // OC Live Dashboard Summary
    DashboardSummaryDTO getDashboardSummary(Long planId);
}
