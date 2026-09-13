package com.registration.management.refreshment.serviceImpl;

import com.registration.management.audit.entities.AuditLog;
import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.checkin.entity.Checkin;
import com.registration.management.checkin.enums.CheckinStatus;
import com.registration.management.checkin.repository.CheckinRepository;
import com.registration.management.common.exception.ResourceNotFoundException;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
import com.registration.management.enums.StaffRole;
import com.registration.management.event.entities.Event;
import com.registration.management.event.repository.EventRepository;
import com.registration.management.participant.entity.Participant;
import com.registration.management.participant.entity.ParticipantEvent;
import com.registration.management.participant.repository.ParticipantEventRepository;
import com.registration.management.participant.repository.ParticipantRepository;
import com.registration.management.refreshment.dto.*;
import com.registration.management.refreshment.entity.*;
import com.registration.management.refreshment.enums.DistributionStatus;
import com.registration.management.refreshment.enums.RecipientCategory;
import com.registration.management.refreshment.enums.TrackingType;
import com.registration.management.refreshment.repository.*;
import com.registration.management.refreshment.service.RefreshmentService;
import com.registration.management.school.entity.SchoolStaff;
import com.registration.management.school.repository.schoolStaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshmentServiceImpl implements RefreshmentService {

    private final RefreshmentPlanRepository planRepository;
    private final RefreshmentSessionRepository sessionRepository;
    private final RefreshmentTeamRepository teamRepository;
    private final RefreshmentTeamMemberRepository teamMemberRepository;
    private final RefreshmentAssignmentRepository assignmentRepository;
    private final RefreshmentDistributionRepository distributionRepository;
    private final GuestRecordRepository guestRecordRepository;
    private final GuestCountDistributionRepository guestCountRepository;

    private final ParticipantRepository participantRepository;
    private final ParticipantEventRepository participantEventRepository;
    private final schoolStaffRepository staffRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CheckinRepository checkinRepository;
    private final AuditLogRepository auditLogRepository;

    // =========================================================================
    // 1. PLAN MANAGEMENT
    // =========================================================================

    @Override
    public RefreshmentPlanResponseDTO createPlan(RefreshmentPlanRequestDTO request, User actor) {
        RefreshmentPlan plan = RefreshmentPlan.builder()
                .name(request.getName().trim())
                .eventDate(request.getEventDate())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        RefreshmentPlan saved = planRepository.save(plan);
        logAudit(actor, "RefreshmentPlan", saved.getId(), AuditAction.REFRESHMENT_PLAN_CREATED, AuditStatus.SUCCESS, "Created Refreshment Plan: " + saved.getName());
        return mapToPlanResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshmentPlanResponseDTO getPlanById(Long id) {
        RefreshmentPlan plan = planRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refreshment plan not found with ID: " + id));
        return mapToPlanResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshmentPlanResponseDTO> getAllPlans() {
        return planRepository.findByActiveTrueOrderByEventDateDesc().stream()
                .map(this::mapToPlanResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RefreshmentPlanResponseDTO updatePlan(Long id, RefreshmentPlanRequestDTO request, User actor) {
        RefreshmentPlan plan = planRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refreshment plan not found with ID: " + id));

        plan.setName(request.getName().trim());
        plan.setEventDate(request.getEventDate());
        plan.setDescription(request.getDescription());
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }
        plan.setUpdatedBy(actor);

        RefreshmentPlan updated = planRepository.save(plan);
        logAudit(actor, "RefreshmentPlan", updated.getId(), AuditAction.REFRESHMENT_PLAN_UPDATED, AuditStatus.SUCCESS, "Updated Refreshment Plan: " + updated.getName());
        return mapToPlanResponse(updated);
    }

    @Override
    public void deletePlan(Long id, User actor) {
        RefreshmentPlan plan = planRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refreshment plan not found with ID: " + id));
        plan.setActive(false);
        plan.setUpdatedBy(actor);
        planRepository.save(plan);
        logAudit(actor, "RefreshmentPlan", id, AuditAction.REFRESHMENT_PLAN_DELETED, AuditStatus.SUCCESS, "Soft deleted Refreshment Plan: " + plan.getName());
    }

    // =========================================================================
    // 2. SESSION MANAGEMENT
    // =========================================================================

    @Override
    public RefreshmentSessionResponseDTO createSession(RefreshmentSessionRequestDTO request, User actor) {
        RefreshmentPlan plan = planRepository.findByIdAndActiveTrue(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Refreshment plan not found with ID: " + request.getPlanId()));

        Event event = null;
        if (request.getEventId() != null) {
            event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + request.getEventId()));
        }

        RefreshmentSession session = RefreshmentSession.builder()
                .plan(plan)
                .name(request.getName().trim())
                .event(event)
                .recipientCategory(request.getRecipientCategory())
                .trackingType(request.getTrackingType() != null ? request.getTrackingType() : TrackingType.INDIVIDUAL)
                .expectedCount(request.getExpectedCount() != null ? request.getExpectedCount() : 0)
                .countDistributed(0)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 1)
                .requireCheckinPresence(request.getRequireCheckinPresence() != null ? request.getRequireCheckinPresence() : true)
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        RefreshmentSession saved = sessionRepository.save(session);
        logAudit(actor, "RefreshmentSession", saved.getId(), AuditAction.REFRESHMENT_SESSION_CREATED, AuditStatus.SUCCESS, "Created Refreshment Session: " + saved.getName());
        return mapToSessionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshmentSessionResponseDTO getSessionById(Long id) {
        RefreshmentSession session = sessionRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refreshment session not found with ID: " + id));
        return mapToSessionResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshmentSessionResponseDTO> getSessionsByPlan(Long planId) {
        return sessionRepository.findByPlanIdAndActiveTrueOrderByDisplayOrderAsc(planId).stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RefreshmentSessionResponseDTO updateSession(Long id, RefreshmentSessionRequestDTO request, User actor) {
        RefreshmentSession session = sessionRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refreshment session not found with ID: " + id));

        Event event = null;
        if (request.getEventId() != null) {
            event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + request.getEventId()));
        }

        session.setName(request.getName().trim());
        session.setEvent(event);
        session.setRecipientCategory(request.getRecipientCategory());
        if (request.getTrackingType() != null) {
            session.setTrackingType(request.getTrackingType());
        }
        if (request.getExpectedCount() != null) {
            session.setExpectedCount(request.getExpectedCount());
        }
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        if (request.getDisplayOrder() != null) {
            session.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getRequireCheckinPresence() != null) {
            session.setRequireCheckinPresence(request.getRequireCheckinPresence());
        }
        if (request.getActive() != null) {
            session.setActive(request.getActive());
        }
        session.setUpdatedBy(actor);

        RefreshmentSession updated = sessionRepository.save(session);
        logAudit(actor, "RefreshmentSession", updated.getId(), AuditAction.REFRESHMENT_SESSION_UPDATED, AuditStatus.SUCCESS, "Updated Refreshment Session: " + updated.getName());
        return mapToSessionResponse(updated);
    }

    @Override
    public void deleteSession(Long id, User actor) {
        RefreshmentSession session = sessionRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refreshment session not found with ID: " + id));
        session.setActive(false);
        session.setUpdatedBy(actor);
        sessionRepository.save(session);
        logAudit(actor, "RefreshmentSession", id, AuditAction.REFRESHMENT_SESSION_DELETED, AuditStatus.SUCCESS, "Soft deleted Refreshment Session: " + session.getName());
    }

    // =========================================================================
    // 3. TEAM MANAGEMENT & ASSIGNMENTS
    // =========================================================================

    @Override
    public RefreshmentTeamResponseDTO createTeam(RefreshmentTeamRequestDTO request, User actor) {
        User leader = null;
        if (request.getLeaderId() != null) {
            leader = userRepository.findById(request.getLeaderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Leader user not found with ID: " + request.getLeaderId()));
        }

        RefreshmentTeam team = RefreshmentTeam.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .leader(leader)
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        RefreshmentTeam saved = teamRepository.save(team);

        if (request.getMemberUserIds() != null && !request.getMemberUserIds().isEmpty()) {
            for (Long userId : request.getMemberUserIds()) {
                User memberUser = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Member user not found with ID: " + userId));
                RefreshmentTeamMember member = RefreshmentTeamMember.builder()
                        .team(saved)
                        .user(memberUser)
                        .build();
                teamMemberRepository.save(member);
            }
        }

        logAudit(actor, "RefreshmentTeam", saved.getId(), AuditAction.REFRESHMENT_TEAM_CREATED, AuditStatus.SUCCESS, "Created Refreshment Team: " + saved.getName());
        return mapToTeamResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshmentTeamResponseDTO getTeamById(Long id) {
        RefreshmentTeam team = teamRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refreshment team not found with ID: " + id));
        return mapToTeamResponse(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshmentTeamResponseDTO> getAllTeams() {
        return teamRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::mapToTeamResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RefreshmentTeamResponseDTO updateTeam(Long id, RefreshmentTeamRequestDTO request, User actor) {
        RefreshmentTeam team = teamRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refreshment team not found with ID: " + id));

        User leader = null;
        if (request.getLeaderId() != null) {
            leader = userRepository.findById(request.getLeaderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Leader user not found with ID: " + request.getLeaderId()));
        }

        team.setName(request.getName().trim());
        team.setDescription(request.getDescription());
        team.setLeader(leader);
        if (request.getActive() != null) {
            team.setActive(request.getActive());
        }
        team.setUpdatedBy(actor);

        if (request.getMemberUserIds() != null) {
            teamMemberRepository.deleteByTeamId(team.getId());
            for (Long userId : request.getMemberUserIds()) {
                User memberUser = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Member user not found with ID: " + userId));
                RefreshmentTeamMember member = RefreshmentTeamMember.builder()
                        .team(team)
                        .user(memberUser)
                        .build();
                teamMemberRepository.save(member);
            }
        }

        RefreshmentTeam updated = teamRepository.save(team);
        logAudit(actor, "RefreshmentTeam", updated.getId(), AuditAction.REFRESHMENT_TEAM_UPDATED, AuditStatus.SUCCESS, "Updated Refreshment Team: " + updated.getName());
        return mapToTeamResponse(updated);
    }

    @Override
    public void deleteTeam(Long id, User actor) {
        RefreshmentTeam team = teamRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refreshment team not found with ID: " + id));
        team.setActive(false);
        team.setUpdatedBy(actor);
        teamRepository.save(team);
        logAudit(actor, "RefreshmentTeam", id, AuditAction.DELETE, AuditStatus.SUCCESS, "Soft deleted Refreshment Team: " + team.getName());
    }

    @Override
    public void assignTeamToSession(RefreshmentAssignmentRequestDTO request, User actor) {
        RefreshmentSession session = sessionRepository.findByIdAndActiveTrue(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + request.getSessionId()));
        RefreshmentTeam team = teamRepository.findByIdAndActiveTrue(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + request.getTeamId()));

        Optional<RefreshmentAssignment> existing = assignmentRepository.findBySessionIdAndTeamId(session.getId(), team.getId());
        if (existing.isPresent()) {
            RefreshmentAssignment a = existing.get();
            a.setActive(true);
            assignmentRepository.save(a);
        } else {
            RefreshmentAssignment assignment = RefreshmentAssignment.builder()
                    .session(session)
                    .team(team)
                    .active(true)
                    .createdBy(actor)
                    .build();
            assignmentRepository.save(assignment);
        }

        logAudit(actor, "RefreshmentAssignment", session.getId(), AuditAction.REFRESHMENT_ASSIGNMENT_CREATED, AuditStatus.SUCCESS,
                "Assigned Team " + team.getName() + " to Session " + session.getName());
    }

    @Override
    public void removeTeamAssignment(Long sessionId, Long teamId, User actor) {
        Optional<RefreshmentAssignment> existing = assignmentRepository.findBySessionIdAndTeamId(sessionId, teamId);
        existing.ifPresent(assignment -> {
            assignmentRepository.delete(assignment);
            logAudit(actor, "RefreshmentAssignment", sessionId, AuditAction.DELETE, AuditStatus.SUCCESS,
                    "Removed Assignment of Team " + teamId + " from Session " + sessionId);
        });
    }

    // =========================================================================
    // 4. TEAM PORTAL (FIELD OPERATIONS)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<TeamSessionSummaryDTO> getMyAssignedSessions(User actor) {
        List<RefreshmentSession> sessions;
        boolean isAdminOrManager = actor.getRole() != null &&
                (actor.getRole().getRoleCode().equalsIgnoreCase("SUPER_ADMIN") ||
                 actor.getRole().getRoleCode().equalsIgnoreCase("ADMIN") ||
                 actor.getRole().getRoleCode().equalsIgnoreCase("EVENT_MANAGER"));

        if (isAdminOrManager) {
            // Admins see all active sessions
            sessions = sessionRepository.findAll().stream()
                    .filter(RefreshmentSession::getActive)
                    .sorted(Comparator.comparing(RefreshmentSession::getDisplayOrder))
                    .collect(Collectors.toList());
        } else {
            sessions = sessionRepository.findAssignedSessionsByUserId(actor.getId());
        }

        List<TeamSessionSummaryDTO> summaries = new ArrayList<>();
        for (RefreshmentSession s : sessions) {
            SessionCounts counts = computeSessionCounts(s);
            summaries.add(TeamSessionSummaryDTO.builder()
                    .sessionId(s.getId())
                    .sessionName(s.getName())
                    .eventId(s.getEvent() != null ? s.getEvent().getId() : null)
                    .eventName(s.getEvent() != null ? s.getEvent().getEventName() : "Festival Wide")
                    .recipientCategory(s.getRecipientCategory())
                    .trackingType(s.getTrackingType())
                    .startTime(s.getStartTime())
                    .endTime(s.getEndTime())
                    .displayOrder(s.getDisplayOrder())
                    .eligibleCount(counts.eligible)
                    .presentCount(counts.present)
                    .distributedCount(counts.distributed)
                    .pendingCount(counts.pending)
                    .expectedCount(s.getExpectedCount())
                    .countDistributed(s.getCountDistributed())
                    .build());
        }
        return summaries;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipientDistributionDTO> getSessionRecipients(Long sessionId, String statusFilter, String search) {
        RefreshmentSession session = sessionRepository.findByIdAndActiveTrue(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        List<RecipientDistributionDTO> allRecipients = loadRecipientsForSession(session);

        return allRecipients.stream()
                .filter(r -> {
                    if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL")) {
                        return r.getRefreshmentStatus().equalsIgnoreCase(statusFilter);
                    }
                    return true;
                })
                .filter(r -> {
                    if (search != null && !search.trim().isEmpty()) {
                        String query = search.trim().toLowerCase();
                        boolean matchesName = r.getName() != null && r.getName().toLowerCase().contains(query);
                        boolean matchesSchool = r.getSchoolName() != null && r.getSchoolName().toLowerCase().contains(query);
                        return matchesName || matchesSchool;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 5. DISTRIBUTION EXECUTION
    // =========================================================================

    @Override
    public DistributionResponseDTO markGiven(MarkGivenRequestDTO request, User actor) {
        RefreshmentSession session = sessionRepository.findByIdAndActiveTrue(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + request.getSessionId()));

        Participant participant = null;
        SchoolStaff staff = null;
        User ocMember = null;
        GuestRecord guest = null;
        String recipientName = "";
        String schoolName = null;
        String eventName = session.getEvent() != null ? session.getEvent().getEventName() : "Festival Wide";

        switch (request.getRecipientCategory()) {
            case PARTICIPANT:
                participant = participantRepository.findById(request.getRecipientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Participant not found with ID: " + request.getRecipientId()));
                recipientName = participant.getFullName();
                if (participant.getRegistration() != null && participant.getRegistration().getSchool() != null) {
                    schoolName = participant.getRegistration().getSchool().getSchoolName();
                }

                // Presence check
                if (Boolean.TRUE.equals(session.getRequireCheckinPresence())) {
                    Optional<Checkin> checkinOpt = checkinRepository.findByParticipantId(participant.getId());
                    if (checkinOpt.isEmpty() || checkinOpt.get().getStatus() != CheckinStatus.CHECKED_IN) {
                        throw new IllegalArgumentException("Participant '" + recipientName + "' is marked ABSENT in Check-in. Cannot distribute refreshment.");
                    }
                }

                // Duplicate / Idempotency check
                Optional<RefreshmentDistribution> existingP = distributionRepository
                        .findBySessionIdAndParticipantIdAndStatus(session.getId(), participant.getId(), DistributionStatus.GIVEN);
                if (existingP.isPresent()) {
                    throw new IllegalStateException("Refreshment has already been distributed to participant: " + recipientName);
                }
                break;

            case LOGIN_TEACHER:
            case ACCOMPANYING_TEACHER:
                staff = staffRepository.findById(request.getRecipientId())
                        .orElseThrow(() -> new ResourceNotFoundException("School staff not found with ID: " + request.getRecipientId()));
                recipientName = staff.getFullName();
                if (staff.getSchool() != null) {
                    schoolName = staff.getSchool().getSchoolName();
                }

                Optional<RefreshmentDistribution> existingS = distributionRepository
                        .findBySessionIdAndSchoolStaffIdAndStatus(session.getId(), staff.getId(), DistributionStatus.GIVEN);
                if (existingS.isPresent()) {
                    throw new IllegalStateException("Refreshment has already been distributed to staff: " + recipientName);
                }
                break;

            case OC_MEMBER:
                ocMember = userRepository.findById(request.getRecipientId())
                        .orElseThrow(() -> new ResourceNotFoundException("OC Member user not found with ID: " + request.getRecipientId()));
                recipientName = ocMember.getFullName();

                Optional<RefreshmentDistribution> existingOC = distributionRepository
                        .findBySessionIdAndOcMemberIdAndStatus(session.getId(), ocMember.getId(), DistributionStatus.GIVEN);
                if (existingOC.isPresent()) {
                    throw new IllegalStateException("Refreshment has already been distributed to OC Member: " + recipientName);
                }
                break;

            case GUEST:
                guest = guestRecordRepository.findByIdAndActiveTrue(request.getRecipientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Guest record not found with ID: " + request.getRecipientId()));
                recipientName = guest.getName();
                schoolName = guest.getOrganization();

                Optional<RefreshmentDistribution> existingG = distributionRepository
                        .findBySessionIdAndGuestRecordIdAndStatus(session.getId(), guest.getId(), DistributionStatus.GIVEN);
                if (existingG.isPresent()) {
                    throw new IllegalStateException("Refreshment has already been distributed to guest: " + recipientName);
                }
                break;
        }

        RefreshmentDistribution distribution = RefreshmentDistribution.builder()
                .session(session)
                .recipientCategory(request.getRecipientCategory())
                .participant(participant)
                .schoolStaff(staff)
                .ocMember(ocMember)
                .guestRecord(guest)
                .status(DistributionStatus.GIVEN)
                .distributedBy(actor)
                .distributedAt(LocalDateTime.now())
                .remarks(request.getRemarks())
                .build();

        RefreshmentDistribution saved = distributionRepository.save(distribution);

        // Update session count
        session.setCountDistributed((session.getCountDistributed() != null ? session.getCountDistributed() : 0) + 1);
        sessionRepository.save(session);

        logAudit(actor, "RefreshmentDistribution", saved.getId(), AuditAction.REFRESHMENT_DISTRIBUTED, AuditStatus.SUCCESS,
                "Distributed refreshment to " + request.getRecipientCategory() + ": " + recipientName + " in session: " + session.getName());

        return mapToDistributionResponse(saved, recipientName, schoolName, eventName);
    }

    @Override
    public List<DistributionResponseDTO> markGivenBulk(MarkGivenBulkRequestDTO request, User actor) {
        List<DistributionResponseDTO> responses = new ArrayList<>();
        for (Long id : request.getRecipientIds()) {
            MarkGivenRequestDTO single = MarkGivenRequestDTO.builder()
                    .sessionId(request.getSessionId())
                    .recipientCategory(request.getRecipientCategory())
                    .recipientId(id)
                    .remarks(request.getRemarks())
                    .build();
            responses.add(markGiven(single, actor));
        }
        return responses;
    }

    @Override
    public RefreshmentSessionResponseDTO incrementGuestCount(Long sessionId, GuestCountIncrementDTO request, User actor) {
        RefreshmentSession session = sessionRepository.findByIdAndActiveTrue(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        if (session.getTrackingType() != TrackingType.COUNT_BASED) {
            throw new IllegalArgumentException("Session is not configured for COUNT_BASED tracking.");
        }

        GuestCountDistribution countDist = GuestCountDistribution.builder()
                .session(session)
                .countDistributed(request.getIncrementBy())
                .distributedBy(actor)
                .distributedAt(LocalDateTime.now())
                .remarks(request.getRemarks())
                .build();

        guestCountRepository.save(countDist);

        session.setCountDistributed((session.getCountDistributed() != null ? session.getCountDistributed() : 0) + request.getIncrementBy());
        RefreshmentSession updated = sessionRepository.save(session);

        logAudit(actor, "GuestCountDistribution", session.getId(), AuditAction.REFRESHMENT_DISTRIBUTED, AuditStatus.SUCCESS,
                "Incremented guest count by " + request.getIncrementBy() + " in session: " + session.getName());

        return mapToSessionResponse(updated);
    }

    // =========================================================================
    // 6. CORRECTION & AUDIT (NO DELETIONS)
    // =========================================================================

    @Override
    public DistributionResponseDTO correctDistribution(Long distributionId, CorrectionRequestDTO request, User actor) {
        RefreshmentDistribution dist = distributionRepository.findById(distributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Distribution record not found with ID: " + distributionId));

        if (dist.getStatus() == DistributionStatus.CORRECTED_REVOKED) {
            throw new IllegalStateException("Distribution record is already revoked/corrected.");
        }

        dist.setStatus(DistributionStatus.CORRECTED_REVOKED);
        dist.setCorrectionReason(request.getReason());
        dist.setCorrectedBy(actor);
        dist.setCorrectedAt(LocalDateTime.now());

        RefreshmentDistribution updated = distributionRepository.save(dist);

        // Adjust session counter
        RefreshmentSession session = dist.getSession();
        if (session.getCountDistributed() != null && session.getCountDistributed() > 0) {
            session.setCountDistributed(session.getCountDistributed() - 1);
            sessionRepository.save(session);
        }

        logAudit(actor, "RefreshmentDistribution", updated.getId(), AuditAction.REFRESHMENT_CORRECTED, AuditStatus.SUCCESS,
                "Revoked distribution ID " + updated.getId() + ". Reason: " + request.getReason());

        String recipientName = getRecipientName(updated);
        String schoolName = getRecipientSchoolName(updated);
        String eventName = session.getEvent() != null ? session.getEvent().getEventName() : "Festival Wide";

        return mapToDistributionResponse(updated, recipientName, schoolName, eventName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributionResponseDTO> getDistributionHistory(Long distributionId) {
        RefreshmentDistribution dist = distributionRepository.findById(distributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Distribution record not found with ID: " + distributionId));

        String recipientName = getRecipientName(dist);
        String schoolName = getRecipientSchoolName(dist);
        String eventName = dist.getSession().getEvent() != null ? dist.getSession().getEvent().getEventName() : "Festival Wide";

        return Collections.singletonList(mapToDistributionResponse(dist, recipientName, schoolName, eventName));
    }

    // =========================================================================
    // 7. SEARCH & UNIVERSAL RECIPIENT LOOKUP
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<RecipientLookupDTO> lookupRecipient(String query, Long planId) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cleanQuery = query.trim().toLowerCase();
        List<RecipientLookupDTO> results = new ArrayList<>();

        // 1. Search Participants
        List<Participant> participants = participantRepository.findAll().stream()
                .filter(p -> p.getFullName() != null && p.getFullName().toLowerCase().contains(cleanQuery))
                .limit(20)
                .collect(Collectors.toList());

        for (Participant p : participants) {
            Optional<Checkin> checkinOpt = checkinRepository.findByParticipantId(p.getId());
            String attendanceStatus = checkinOpt.map(c -> c.getStatus().name()).orElse("ABSENT");
            String schoolName = (p.getRegistration() != null && p.getRegistration().getSchool() != null)
                    ? p.getRegistration().getSchool().getSchoolName() : null;

            List<ParticipantEvent> pEvents = participantEventRepository.findByParticipantId(p.getId());
            String eventName = pEvents.isEmpty() ? "General" : pEvents.get(0).getEvent().getEventName();
            Long eventId = pEvents.isEmpty() ? null : pEvents.get(0).getEvent().getId();

            List<RefreshmentDistribution> distributions = distributionRepository
                    .findByParticipantIdAndStatus(p.getId(), DistributionStatus.GIVEN);

            List<RecipientLookupDTO.SessionStatusDTO> sessionStatuses = distributions.stream()
                    .map(d -> RecipientLookupDTO.SessionStatusDTO.builder()
                            .sessionId(d.getSession().getId())
                            .sessionName(d.getSession().getName())
                            .refreshmentStatus(d.getStatus().name())
                            .distributionId(d.getId())
                            .distributedByName(d.getDistributedBy() != null ? d.getDistributedBy().getFullName() : null)
                            .distributedAt(d.getDistributedAt() != null ? d.getDistributedAt().toString() : null)
                            .build())
                    .collect(Collectors.toList());

            results.add(RecipientLookupDTO.builder()
                    .recipientId(p.getId())
                    .recipientCategory(RecipientCategory.PARTICIPANT)
                    .name(p.getFullName())
                    .schoolName(schoolName)
                    .eventId(eventId)
                    .eventName(eventName)
                    .attendanceStatus(attendanceStatus)
                    .sessions(sessionStatuses)
                    .build());
        }

        // 2. Search Staff
        List<SchoolStaff> staffList = staffRepository.findAll().stream()
                .filter(s -> s.getFullName() != null && s.getFullName().toLowerCase().contains(cleanQuery))
                .limit(10)
                .collect(Collectors.toList());

        for (SchoolStaff s : staffList) {
            RecipientCategory cat = (s.getStaffRole() == StaffRole.ACCOMPANYING_TEACHER)
                    ? RecipientCategory.ACCOMPANYING_TEACHER : RecipientCategory.LOGIN_TEACHER;
            String schoolName = s.getSchool() != null ? s.getSchool().getSchoolName() : null;

            List<RefreshmentDistribution> distributions = distributionRepository
                    .findBySchoolStaffIdAndStatus(s.getId(), DistributionStatus.GIVEN);

            List<RecipientLookupDTO.SessionStatusDTO> sessionStatuses = distributions.stream()
                    .map(d -> RecipientLookupDTO.SessionStatusDTO.builder()
                            .sessionId(d.getSession().getId())
                            .sessionName(d.getSession().getName())
                            .refreshmentStatus(d.getStatus().name())
                            .distributionId(d.getId())
                            .distributedByName(d.getDistributedBy() != null ? d.getDistributedBy().getFullName() : null)
                            .distributedAt(d.getDistributedAt() != null ? d.getDistributedAt().toString() : null)
                            .build())
                    .collect(Collectors.toList());

            results.add(RecipientLookupDTO.builder()
                    .recipientId(s.getId())
                    .recipientCategory(cat)
                    .name(s.getFullName())
                    .schoolName(schoolName)
                    .attendanceStatus("PRESENT")
                    .sessions(sessionStatuses)
                    .build());
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipientDistributionDTO> getGlobalRecipients(Long planId, Long eventId, RecipientCategory category, String status, Long schoolId, String search) {
        List<RefreshmentSession> sessions;
        if (planId != null) {
            sessions = sessionRepository.findByPlanIdAndActiveTrueOrderByDisplayOrderAsc(planId);
        } else {
            sessions = sessionRepository.findAll().stream().filter(RefreshmentSession::getActive).collect(Collectors.toList());
        }

        List<RecipientDistributionDTO> masterList = new ArrayList<>();
        for (RefreshmentSession s : sessions) {
            if (eventId != null && s.getEvent() != null && !s.getEvent().getId().equals(eventId)) {
                continue;
            }
            if (category != null && s.getRecipientCategory() != category) {
                continue;
            }
            masterList.addAll(loadRecipientsForSession(s));
        }

        return masterList.stream()
                .filter(r -> {
                    if (status != null && !status.equalsIgnoreCase("ALL")) {
                        return r.getRefreshmentStatus().equalsIgnoreCase(status);
                    }
                    return true;
                })
                .filter(r -> {
                    if (search != null && !search.trim().isEmpty()) {
                        String query = search.trim().toLowerCase();
                        boolean matchesName = r.getName() != null && r.getName().toLowerCase().contains(query);
                        boolean matchesSchool = r.getSchoolName() != null && r.getSchoolName().toLowerCase().contains(query);
                        return matchesName || matchesSchool;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 8. GUEST RECORDS
    // =========================================================================

    @Override
    public GuestRecordResponseDTO createGuestRecord(GuestRecordRequestDTO request, User actor) {
        RefreshmentPlan plan = planRepository.findByIdAndActiveTrue(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + request.getPlanId()));

        GuestRecord guest = GuestRecord.builder()
                .plan(plan)
                .name(request.getName().trim())
                .designation(request.getDesignation())
                .organization(request.getOrganization())
                .contactNumber(request.getContactNumber())
                .remarks(request.getRemarks())
                .active(true)
                .build();

        GuestRecord saved = guestRecordRepository.save(guest);
        logAudit(actor, "GuestRecord", saved.getId(), AuditAction.CREATE, AuditStatus.SUCCESS, "Created Guest Record: " + saved.getName());
        return mapToGuestResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestRecordResponseDTO> getGuestsByPlan(Long planId) {
        return guestRecordRepository.findByPlanIdAndActiveTrue(planId).stream()
                .map(this::mapToGuestResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteGuestRecord(Long id, User actor) {
        GuestRecord guest = guestRecordRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest record not found with ID: " + id));
        guest.setActive(false);
        guestRecordRepository.save(guest);
        logAudit(actor, "GuestRecord", id, AuditAction.DELETE, AuditStatus.SUCCESS, "Deleted Guest Record: " + guest.getName());
    }

    // =========================================================================
    // 9. OC DASHBOARD LIVE SUMMARY
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary(Long planId) {
        RefreshmentPlan plan = null;
        if (planId != null) {
            plan = planRepository.findByIdAndActiveTrue(planId).orElse(null);
        } else {
            List<RefreshmentPlan> plans = planRepository.findByActiveTrueOrderByEventDateDesc();
            if (!plans.isEmpty()) {
                plan = plans.get(0);
            }
        }

        if (plan == null) {
            return DashboardSummaryDTO.builder()
                    .totalEligible(0L)
                    .totalPresent(0L)
                    .totalDistributed(0L)
                    .totalPending(0L)
                    .overallDistributionRate(0.0)
                    .categoryWise(Collections.emptyList())
                    .eventWise(Collections.emptyList())
                    .build();
        }

        List<RefreshmentSession> sessions = sessionRepository.findByPlanIdAndActiveTrueOrderByDisplayOrderAsc(plan.getId());

        long totalEligible = 0;
        long totalPresent = 0;
        long totalDistributed = 0;

        Map<RecipientCategory, CategoryAccumulator> categoryMap = new EnumMap<>(RecipientCategory.class);
        for (RecipientCategory cat : RecipientCategory.values()) {
            categoryMap.put(cat, new CategoryAccumulator());
        }

        Map<Long, EventAccumulator> eventMap = new HashMap<>();

        for (RefreshmentSession s : sessions) {
            SessionCounts counts = computeSessionCounts(s);
            totalEligible += counts.eligible;
            totalPresent += counts.present;
            totalDistributed += counts.distributed;

            CategoryAccumulator catAcc = categoryMap.get(s.getRecipientCategory());
            catAcc.eligible += counts.eligible;
            catAcc.present += counts.present;
            catAcc.distributed += counts.distributed;

            if (s.getEvent() != null) {
                EventAccumulator evAcc = eventMap.computeIfAbsent(s.getEvent().getId(), k -> new EventAccumulator(s.getEvent().getId(), s.getEvent().getEventName()));
                evAcc.eligible += counts.eligible;
                evAcc.present += counts.present;
                evAcc.distributed += counts.distributed;
            }
        }

        long totalPending = Math.max(0, totalPresent - totalDistributed);
        double overallRate = totalPresent > 0 ? ((double) totalDistributed / totalPresent) * 100.0 : 0.0;

        List<CategorySummaryDTO> categoryWise = new ArrayList<>();
        for (Map.Entry<RecipientCategory, CategoryAccumulator> entry : categoryMap.entrySet()) {
            CategoryAccumulator acc = entry.getValue();
            long pending = Math.max(0, acc.present - acc.distributed);
            double rate = acc.present > 0 ? ((double) acc.distributed / acc.present) * 100.0 : 0.0;
            categoryWise.add(CategorySummaryDTO.builder()
                    .category(entry.getKey())
                    .eligible(acc.eligible)
                    .present(acc.present)
                    .distributed(acc.distributed)
                    .pending(pending)
                    .distributionRate(Math.round(rate * 10.0) / 10.0)
                    .build());
        }

        List<EventSummaryDTO> eventWise = new ArrayList<>();
        for (EventAccumulator acc : eventMap.values()) {
            long pending = Math.max(0, acc.present - acc.distributed);
            double rate = acc.present > 0 ? ((double) acc.distributed / acc.present) * 100.0 : 0.0;
            eventWise.add(EventSummaryDTO.builder()
                    .eventId(acc.eventId)
                    .eventName(acc.eventName)
                    .eligible(acc.eligible)
                    .present(acc.present)
                    .distributed(acc.distributed)
                    .pending(pending)
                    .distributionRate(Math.round(rate * 10.0) / 10.0)
                    .build());
        }

        return DashboardSummaryDTO.builder()
                .planId(plan.getId())
                .planName(plan.getName())
                .totalEligible(totalEligible)
                .totalPresent(totalPresent)
                .totalDistributed(totalDistributed)
                .totalPending(totalPending)
                .overallDistributionRate(Math.round(overallRate * 10.0) / 10.0)
                .categoryWise(categoryWise)
                .eventWise(eventWise)
                .build();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private static class CategoryAccumulator {
        long eligible = 0;
        long present = 0;
        long distributed = 0;
    }

    private static class EventAccumulator {
        Long eventId;
        String eventName;
        long eligible = 0;
        long present = 0;
        long distributed = 0;

        EventAccumulator(Long eventId, String eventName) {
            this.eventId = eventId;
            this.eventName = eventName;
        }
    }

    private static class SessionCounts {
        long eligible = 0;
        long present = 0;
        long distributed = 0;
        long pending = 0;
    }

    private SessionCounts computeSessionCounts(RefreshmentSession session) {
        SessionCounts counts = new SessionCounts();

        if (session.getTrackingType() == TrackingType.COUNT_BASED) {
            counts.eligible = session.getExpectedCount() != null ? session.getExpectedCount() : 0;
            counts.present = counts.eligible;
            counts.distributed = session.getCountDistributed() != null ? session.getCountDistributed() : 0;
            counts.pending = Math.max(0, counts.eligible - counts.distributed);
            return counts;
        }

        List<RecipientDistributionDTO> recipients = loadRecipientsForSession(session);
        counts.eligible = recipients.size();
        counts.present = recipients.stream().filter(r -> "PRESENT".equalsIgnoreCase(r.getAttendanceStatus())).count();
        counts.distributed = recipients.stream().filter(r -> "GIVEN".equalsIgnoreCase(r.getRefreshmentStatus())).count();
        counts.pending = Math.max(0, counts.present - counts.distributed);
        return counts;
    }

    private List<RecipientDistributionDTO> loadRecipientsForSession(RefreshmentSession session) {
        List<RecipientDistributionDTO> list = new ArrayList<>();
        RecipientCategory cat = session.getRecipientCategory();
        Long eventId = session.getEvent() != null ? session.getEvent().getId() : null;
        String eventName = session.getEvent() != null ? session.getEvent().getEventName() : "Festival Wide";

        Map<Long, RefreshmentDistribution> distMap = new HashMap<>();
        List<RefreshmentDistribution> existingDists = distributionRepository.findBySessionIdAndStatus(session.getId(), DistributionStatus.GIVEN);

        switch (cat) {
            case PARTICIPANT:
                for (RefreshmentDistribution d : existingDists) {
                    if (d.getParticipant() != null) {
                        distMap.put(d.getParticipant().getId(), d);
                    }
                }

                List<Participant> participants;
                if (eventId != null) {
                    participants = participantEventRepository.findByEventId(eventId).stream()
                            .map(ParticipantEvent::getParticipant)
                            .collect(Collectors.toList());
                } else {
                    participants = participantRepository.findAll();
                }

                for (Participant p : participants) {
                    Optional<Checkin> checkinOpt = checkinRepository.findByParticipantId(p.getId());
                    boolean isPresent = checkinOpt.isPresent() && checkinOpt.get().getStatus() == CheckinStatus.CHECKED_IN;
                    String attendanceStatus = isPresent ? "PRESENT" : "ABSENT";

                    RefreshmentDistribution dist = distMap.get(p.getId());
                    String refreshmentStatus;
                    if (dist != null) {
                        refreshmentStatus = "GIVEN";
                    } else if (Boolean.TRUE.equals(session.getRequireCheckinPresence()) && !isPresent) {
                        refreshmentStatus = "NOT_ELIGIBLE";
                    } else {
                        refreshmentStatus = "PENDING";
                    }

                    String schoolName = (p.getRegistration() != null && p.getRegistration().getSchool() != null)
                            ? p.getRegistration().getSchool().getSchoolName() : null;

                    list.add(RecipientDistributionDTO.builder()
                            .recipientId(p.getId())
                            .recipientCategory(RecipientCategory.PARTICIPANT)
                            .name(p.getFullName())
                            .schoolName(schoolName)
                            .eventId(eventId)
                            .eventName(eventName)
                            .contactNumber(p.getGuardianPhone())
                            .attendanceStatus(attendanceStatus)
                            .refreshmentStatus(refreshmentStatus)
                            .distributionId(dist != null ? dist.getId() : null)
                            .distributionState(dist != null ? dist.getStatus() : null)
                            .distributedByName(dist != null && dist.getDistributedBy() != null ? dist.getDistributedBy().getFullName() : null)
                            .distributedAt(dist != null ? dist.getDistributedAt() : null)
                            .remarks(dist != null ? dist.getRemarks() : null)
                            .eligible(isPresent || !Boolean.TRUE.equals(session.getRequireCheckinPresence()))
                            .build());
                }
                break;

            case LOGIN_TEACHER:
            case ACCOMPANYING_TEACHER:
                StaffRole targetRole = (cat == RecipientCategory.LOGIN_TEACHER) ? StaffRole.LOGIN_TEACHER : StaffRole.ACCOMPANYING_TEACHER;
                for (RefreshmentDistribution d : existingDists) {
                    if (d.getSchoolStaff() != null) {
                        distMap.put(d.getSchoolStaff().getId(), d);
                    }
                }

                List<SchoolStaff> staffList = staffRepository.findAll().stream()
                        .filter(s -> s.isActive() && s.getStaffRole() == targetRole)
                        .collect(Collectors.toList());

                for (SchoolStaff s : staffList) {
                    RefreshmentDistribution dist = distMap.get(s.getId());
                    String refreshmentStatus = dist != null ? "GIVEN" : "PENDING";
                    String schoolName = s.getSchool() != null ? s.getSchool().getSchoolName() : null;

                    list.add(RecipientDistributionDTO.builder()
                            .recipientId(s.getId())
                            .recipientCategory(cat)
                            .name(s.getFullName())
                            .schoolName(schoolName)
                            .eventId(eventId)
                            .eventName(eventName)
                            .contactNumber(s.getPhone())
                            .attendanceStatus("PRESENT")
                            .refreshmentStatus(refreshmentStatus)
                            .distributionId(dist != null ? dist.getId() : null)
                            .distributionState(dist != null ? dist.getStatus() : null)
                            .distributedByName(dist != null && dist.getDistributedBy() != null ? dist.getDistributedBy().getFullName() : null)
                            .distributedAt(dist != null ? dist.getDistributedAt() : null)
                            .remarks(dist != null ? dist.getRemarks() : null)
                            .eligible(true)
                            .build());
                }
                break;

            case OC_MEMBER:
                for (RefreshmentDistribution d : existingDists) {
                    if (d.getOcMember() != null) {
                        distMap.put(d.getOcMember().getId(), d);
                    }
                }

                List<User> ocUsers = userRepository.findByActiveTrue().stream()
                        .filter(u -> u.getRole() != null && !u.getRole().getRoleCode().equalsIgnoreCase("PARTICIPANT"))
                        .collect(Collectors.toList());

                for (User u : ocUsers) {
                    RefreshmentDistribution dist = distMap.get(u.getId());
                    String refreshmentStatus = dist != null ? "GIVEN" : "PENDING";

                    list.add(RecipientDistributionDTO.builder()
                            .recipientId(u.getId())
                            .recipientCategory(RecipientCategory.OC_MEMBER)
                            .name(u.getFullName())
                            .schoolName("Organizing Committee")
                            .eventId(eventId)
                            .eventName(eventName)
                            .contactNumber(null)
                            .attendanceStatus("PRESENT")
                            .refreshmentStatus(refreshmentStatus)
                            .distributionId(dist != null ? dist.getId() : null)
                            .distributionState(dist != null ? dist.getStatus() : null)
                            .distributedByName(dist != null && dist.getDistributedBy() != null ? dist.getDistributedBy().getFullName() : null)
                            .distributedAt(dist != null ? dist.getDistributedAt() : null)
                            .remarks(dist != null ? dist.getRemarks() : null)
                            .eligible(true)
                            .build());
                }
                break;

            case GUEST:
                for (RefreshmentDistribution d : existingDists) {
                    if (d.getGuestRecord() != null) {
                        distMap.put(d.getGuestRecord().getId(), d);
                    }
                }

                List<GuestRecord> guestList = guestRecordRepository.findByPlanIdAndActiveTrue(session.getPlan().getId());
                for (GuestRecord g : guestList) {
                    RefreshmentDistribution dist = distMap.get(g.getId());
                    String refreshmentStatus = dist != null ? "GIVEN" : "PENDING";

                    list.add(RecipientDistributionDTO.builder()
                            .recipientId(g.getId())
                            .recipientCategory(RecipientCategory.GUEST)
                            .name(g.getName())
                            .schoolName(g.getOrganization())
                            .eventId(eventId)
                            .eventName(eventName)
                            .contactNumber(g.getContactNumber())
                            .attendanceStatus("PRESENT")
                            .refreshmentStatus(refreshmentStatus)
                            .distributionId(dist != null ? dist.getId() : null)
                            .distributionState(dist != null ? dist.getStatus() : null)
                            .distributedByName(dist != null && dist.getDistributedBy() != null ? dist.getDistributedBy().getFullName() : null)
                            .distributedAt(dist != null ? dist.getDistributedAt() : null)
                            .remarks(dist != null ? dist.getRemarks() : null)
                            .eligible(true)
                            .build());
                }
                break;
        }

        return list;
    }

    private String getRecipientName(RefreshmentDistribution dist) {
        if (dist.getParticipant() != null) return dist.getParticipant().getFullName();
        if (dist.getSchoolStaff() != null) return dist.getSchoolStaff().getFullName();
        if (dist.getOcMember() != null) return dist.getOcMember().getFullName();
        if (dist.getGuestRecord() != null) return dist.getGuestRecord().getName();
        return "Unknown";
    }

    private String getRecipientSchoolName(RefreshmentDistribution dist) {
        if (dist.getParticipant() != null && dist.getParticipant().getRegistration() != null && dist.getParticipant().getRegistration().getSchool() != null) {
            return dist.getParticipant().getRegistration().getSchool().getSchoolName();
        }
        if (dist.getSchoolStaff() != null && dist.getSchoolStaff().getSchool() != null) {
            return dist.getSchoolStaff().getSchool().getSchoolName();
        }
        if (dist.getGuestRecord() != null) {
            return dist.getGuestRecord().getOrganization();
        }
        return null;
    }

    private RefreshmentPlanResponseDTO mapToPlanResponse(RefreshmentPlan plan) {
        int totalSessions = plan.getSessions() != null ? plan.getSessions().size() : 0;
        return RefreshmentPlanResponseDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .eventDate(plan.getEventDate())
                .description(plan.getDescription())
                .active(plan.getActive())
                .totalSessions(totalSessions)
                .createdById(plan.getCreatedBy() != null ? plan.getCreatedBy().getId() : null)
                .createdByName(plan.getCreatedBy() != null ? plan.getCreatedBy().getFullName() : null)
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    private RefreshmentSessionResponseDTO mapToSessionResponse(RefreshmentSession session) {
        List<String> teamNames = assignmentRepository.findBySessionIdAndActiveTrue(session.getId()).stream()
                .map(a -> a.getTeam().getName())
                .collect(Collectors.toList());

        SessionCounts counts = computeSessionCounts(session);

        return RefreshmentSessionResponseDTO.builder()
                .id(session.getId())
                .planId(session.getPlan().getId())
                .planName(session.getPlan().getName())
                .name(session.getName())
                .eventId(session.getEvent() != null ? session.getEvent().getId() : null)
                .eventName(session.getEvent() != null ? session.getEvent().getEventName() : "Festival Wide")
                .recipientCategory(session.getRecipientCategory())
                .trackingType(session.getTrackingType())
                .expectedCount(session.getExpectedCount())
                .countDistributed(session.getCountDistributed())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .displayOrder(session.getDisplayOrder())
                .requireCheckinPresence(session.getRequireCheckinPresence())
                .active(session.getActive())
                .assignedTeamNames(teamNames)
                .eligibleCount(counts.eligible)
                .presentCount(counts.present)
                .distributedCount(counts.distributed)
                .pendingCount(counts.pending)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private RefreshmentTeamResponseDTO mapToTeamResponse(RefreshmentTeam team) {
        List<RefreshmentTeamResponseDTO.TeamMemberDTO> members = teamMemberRepository.findByTeamId(team.getId()).stream()
                .map(m -> RefreshmentTeamResponseDTO.TeamMemberDTO.builder()
                        .userId(m.getUser().getId())
                        .fullName(m.getUser().getFullName())
                        .email(m.getUser().getEmail())
                        .build())
                .collect(Collectors.toList());

        return RefreshmentTeamResponseDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .leaderId(team.getLeader() != null ? team.getLeader().getId() : null)
                .leaderName(team.getLeader() != null ? team.getLeader().getFullName() : null)
                .active(team.getActive())
                .members(members)
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    private DistributionResponseDTO mapToDistributionResponse(RefreshmentDistribution d, String recipientName, String schoolName, String eventName) {
        Long recipientId = null;
        if (d.getParticipant() != null) recipientId = d.getParticipant().getId();
        else if (d.getSchoolStaff() != null) recipientId = d.getSchoolStaff().getId();
        else if (d.getOcMember() != null) recipientId = d.getOcMember().getId();
        else if (d.getGuestRecord() != null) recipientId = d.getGuestRecord().getId();

        return DistributionResponseDTO.builder()
                .id(d.getId())
                .sessionId(d.getSession().getId())
                .sessionName(d.getSession().getName())
                .recipientCategory(d.getRecipientCategory())
                .recipientId(recipientId)
                .recipientName(recipientName)
                .schoolName(schoolName)
                .eventName(eventName)
                .status(d.getStatus())
                .distributedById(d.getDistributedBy() != null ? d.getDistributedBy().getId() : null)
                .distributedByName(d.getDistributedBy() != null ? d.getDistributedBy().getFullName() : null)
                .distributedAt(d.getDistributedAt())
                .remarks(d.getRemarks())
                .correctionReason(d.getCorrectionReason())
                .correctedById(d.getCorrectedBy() != null ? d.getCorrectedBy().getId() : null)
                .correctedByName(d.getCorrectedBy() != null ? d.getCorrectedBy().getFullName() : null)
                .correctedAt(d.getCorrectedAt())
                .build();
    }

    private GuestRecordResponseDTO mapToGuestResponse(GuestRecord g) {
        return GuestRecordResponseDTO.builder()
                .id(g.getId())
                .planId(g.getPlan().getId())
                .name(g.getName())
                .designation(g.getDesignation())
                .organization(g.getOrganization())
                .contactNumber(g.getContactNumber())
                .remarks(g.getRemarks())
                .active(g.getActive())
                .createdAt(g.getCreatedAt())
                .build();
    }

    private void logAudit(User user, String entityName, Long entityId, AuditAction action, AuditStatus status, String description) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .entityName(entityName)
                    .entityId(entityId)
                    .action(action)
                    .module("REFRESHMENT")
                    .description(description)
                    .status(status)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log for refreshment action: {}", action, e);
        }
    }
}
