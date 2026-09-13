package com.registration.management.refreshment.service;

import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.Role;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.checkin.entity.Checkin;
import com.registration.management.checkin.enums.CheckinStatus;
import com.registration.management.checkin.repository.CheckinRepository;
import com.registration.management.event.entities.Event;
import com.registration.management.event.repository.EventRepository;
import com.registration.management.participant.entity.Participant;
import com.registration.management.participant.repository.ParticipantEventRepository;
import com.registration.management.participant.repository.ParticipantRepository;
import com.registration.management.refreshment.dto.*;
import com.registration.management.refreshment.entity.*;
import com.registration.management.refreshment.enums.DistributionStatus;
import com.registration.management.refreshment.enums.RecipientCategory;
import com.registration.management.refreshment.enums.TrackingType;
import com.registration.management.refreshment.repository.*;
import com.registration.management.refreshment.serviceImpl.RefreshmentServiceImpl;
import com.registration.management.school.repository.schoolStaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshmentServiceImplTest {

    @Mock
    private RefreshmentPlanRepository planRepository;
    @Mock
    private RefreshmentSessionRepository sessionRepository;
    @Mock
    private RefreshmentTeamRepository teamRepository;
    @Mock
    private RefreshmentTeamMemberRepository teamMemberRepository;
    @Mock
    private RefreshmentAssignmentRepository assignmentRepository;
    @Mock
    private RefreshmentDistributionRepository distributionRepository;
    @Mock
    private GuestRecordRepository guestRecordRepository;
    @Mock
    private GuestCountDistributionRepository guestCountRepository;

    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private ParticipantEventRepository participantEventRepository;
    @Mock
    private schoolStaffRepository staffRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private CheckinRepository checkinRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private RefreshmentServiceImpl refreshmentService;

    private User testUser;
    private RefreshmentPlan testPlan;
    private Event testEvent;
    private RefreshmentSession testSession;
    private Participant testParticipant;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .fullName("Admin User")
                .email("admin@test.com")
                .role(Role.builder().roleCode("ADMIN").roleName("Admin").build())
                .build();

        testPlan = RefreshmentPlan.builder()
                .id(10L)
                .name("APEX 2026")
                .eventDate(LocalDate.now())
                .active(true)
                .build();

        testEvent = Event.builder()
                .id(100L)
                .eventName("SQL Master")
                .build();

        testSession = RefreshmentSession.builder()
                .id(500L)
                .plan(testPlan)
                .name("SQL Master Lunch")
                .event(testEvent)
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .trackingType(TrackingType.INDIVIDUAL)
                .requireCheckinPresence(true)
                .countDistributed(0)
                .active(true)
                .build();

        testParticipant = Participant.builder()
                .id(1000L)
                .fullName("Aman Singh")
                .guardianPhone("9876543210")
                .build();
    }

    @Test
    @DisplayName("markGiven - Success when participant is PRESENT")
    void markGiven_Success_WhenPresent() {
        MarkGivenRequestDTO request = MarkGivenRequestDTO.builder()
                .sessionId(500L)
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .recipientId(1000L)
                .remarks("Counter 1")
                .build();

        when(sessionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(testSession));
        when(participantRepository.findById(1000L)).thenReturn(Optional.of(testParticipant));
        when(checkinRepository.findByParticipantId(1000L)).thenReturn(Optional.of(
                Checkin.builder().status(CheckinStatus.CHECKED_IN).build()
        ));
        when(distributionRepository.findBySessionIdAndParticipantIdAndStatus(500L, 1000L, DistributionStatus.GIVEN))
                .thenReturn(Optional.empty());

        when(distributionRepository.save(any(RefreshmentDistribution.class))).thenAnswer(inv -> {
            RefreshmentDistribution d = inv.getArgument(0);
            d.setId(9999L);
            return d;
        });

        DistributionResponseDTO result = refreshmentService.markGiven(request, testUser);

        assertNotNull(result);
        assertEquals(9999L, result.getId());
        assertEquals("Aman Singh", result.getRecipientName());
        assertEquals(DistributionStatus.GIVEN, result.getStatus());
        assertEquals(1, testSession.getCountDistributed());
    }

    @Test
    @DisplayName("markGiven - Throws IllegalArgumentException when participant is ABSENT and presence required")
    void markGiven_Throws_WhenAbsent() {
        MarkGivenRequestDTO request = MarkGivenRequestDTO.builder()
                .sessionId(500L)
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .recipientId(1000L)
                .build();

        when(sessionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(testSession));
        when(participantRepository.findById(1000L)).thenReturn(Optional.of(testParticipant));
        when(checkinRepository.findByParticipantId(1000L)).thenReturn(Optional.empty()); // Not checked in

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                refreshmentService.markGiven(request, testUser));

        assertTrue(ex.getMessage().contains("marked ABSENT in Check-in"));
    }

    @Test
    @DisplayName("markGiven - Throws IllegalStateException on duplicate distribution")
    void markGiven_Throws_OnDuplicate() {
        MarkGivenRequestDTO request = MarkGivenRequestDTO.builder()
                .sessionId(500L)
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .recipientId(1000L)
                .build();

        when(sessionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(testSession));
        when(participantRepository.findById(1000L)).thenReturn(Optional.of(testParticipant));
        when(checkinRepository.findByParticipantId(1000L)).thenReturn(Optional.of(
                Checkin.builder().status(CheckinStatus.CHECKED_IN).build()
        ));
        when(distributionRepository.findBySessionIdAndParticipantIdAndStatus(500L, 1000L, DistributionStatus.GIVEN))
                .thenReturn(Optional.of(RefreshmentDistribution.builder().id(9999L).build()));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                refreshmentService.markGiven(request, testUser));

        assertTrue(ex.getMessage().contains("already been distributed"));
    }

    @Test
    @DisplayName("correctDistribution - Successfully marks as CORRECTED_REVOKED and decrements count")
    void correctDistribution_Success() {
        testSession.setCountDistributed(5);

        RefreshmentDistribution dist = RefreshmentDistribution.builder()
                .id(8888L)
                .session(testSession)
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .participant(testParticipant)
                .status(DistributionStatus.GIVEN)
                .distributedBy(testUser)
                .distributedAt(LocalDateTime.now())
                .build();

        when(distributionRepository.findById(8888L)).thenReturn(Optional.of(dist));
        when(distributionRepository.save(any(RefreshmentDistribution.class))).thenAnswer(inv -> inv.getArgument(0));

        CorrectionRequestDTO request = CorrectionRequestDTO.builder()
                .reason("Mistakenly clicked")
                .build();

        DistributionResponseDTO response = refreshmentService.correctDistribution(8888L, request, testUser);

        assertEquals(DistributionStatus.CORRECTED_REVOKED, response.getStatus());
        assertEquals("Mistakenly clicked", response.getCorrectionReason());
        assertEquals(4, testSession.getCountDistributed());
    }

    @Test
    @DisplayName("incrementGuestCount - Success for COUNT_BASED sessions")
    void incrementGuestCount_Success() {
        RefreshmentSession guestSession = RefreshmentSession.builder()
                .id(600L)
                .plan(testPlan)
                .name("Guest Tea")
                .trackingType(TrackingType.COUNT_BASED)
                .expectedCount(50)
                .countDistributed(10)
                .active(true)
                .build();

        when(sessionRepository.findByIdAndActiveTrue(600L)).thenReturn(Optional.of(guestSession));
        when(sessionRepository.save(any(RefreshmentSession.class))).thenAnswer(inv -> inv.getArgument(0));

        GuestCountIncrementDTO request = GuestCountIncrementDTO.builder()
                .incrementBy(5)
                .remarks("VIP lounge")
                .build();

        RefreshmentSessionResponseDTO res = refreshmentService.incrementGuestCount(600L, request, testUser);

        assertNotNull(res);
        assertEquals(15, res.getCountDistributed());
    }
}
