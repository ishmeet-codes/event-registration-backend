package com.registration.management.checkin.service;

import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.User;
import com.registration.management.checkin.dto.*;
import com.registration.management.checkin.entity.Checkin;
import com.registration.management.checkin.enums.CheckinStatus;
import com.registration.management.checkin.exception.CheckinNotFoundException;
import com.registration.management.checkin.exception.InvalidCheckinStatusException;
import com.registration.management.checkin.exception.ParticipantAlreadyCheckedInException;
import com.registration.management.checkin.repository.CheckinRepository;
import com.registration.management.checkin.serviceImpl.CheckinServiceImpl;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.event.entities.Event;
import com.registration.management.event.repository.EventRepository;
import com.registration.management.participant.entity.Participant;
import com.registration.management.participant.entity.ParticipantEvent;
import com.registration.management.participant.exception.ParticipantNotFoundException;
import com.registration.management.participant.repository.ParticipantEventRepository;
import com.registration.management.participant.repository.ParticipantRepository;
import com.registration.management.registration.entity.Registration;
import com.registration.management.school.entity.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckinServiceImplTest {

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ParticipantEventRepository participantEventRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private CheckinServiceImpl checkinService;

    private User currentActor;
    private School school;
    private Event event;
    private Registration registration;
    private Participant participant;
    private Checkin checkin;

    @BeforeEach
    void setUp() {
        currentActor = User.builder()
                .id(1L)
                .email("admin@test.com")
                .fullName("Admin User")
                .build();

        school = School.builder()
                .id(10L)
                .schoolName("Nankana Sahib Public School")
                .schoolCode("SCH001")
                .build();

        event = Event.builder()
                .id(5L)
                .eventName("SQL Masters")
                .build();

        registration = Registration.builder()
                .id(100L)
                .school(school)
                .status(RegistrationStatus.APPROVED)
                .build();

        participant = Participant.builder()
                .id(101L)
                .fullName("Rahul Sharma")
                .className("11")
                .registration(registration)
                .participantEvents(new HashSet<>())
                .build();

        ParticipantEvent pe = ParticipantEvent.builder()
                .id(1L)
                .participant(participant)
                .event(event)
                .build();

        participant.getParticipantEvents().add(pe);

        checkin = Checkin.builder()
                .id(501L)
                .participant(participant)
                .event(event)
                .registration(registration)
                .school(school)
                .status(CheckinStatus.CHECKED_IN)
                .checkedInAt(LocalDateTime.now())
                .checkedInBy(currentActor)
                .remarks("ID Verified")
                .build();
    }

    @Test
    @DisplayName("checkinParticipant - Success")
    void checkinParticipant_Success() {
        CheckinRequestDTO request = CheckinRequestDTO.builder()
                .participantId(101L)
                .remarks("ID Verified")
                .build();

        when(participantRepository.findById(101L)).thenReturn(Optional.of(participant));
        when(checkinRepository.existsByParticipantId(101L)).thenReturn(false);
        when(checkinRepository.save(any(Checkin.class))).thenReturn(checkin);

        CheckinResponseDTO response = checkinService.checkinParticipant(request, currentActor);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(501L);
        assertThat(response.getParticipant().getName()).isEqualTo("Rahul Sharma");
        assertThat(response.getStatus()).isEqualTo(CheckinStatus.CHECKED_IN);
        verify(checkinRepository, times(1)).save(any(Checkin.class));
    }

    @Test
    @DisplayName("checkinParticipant - Throws ParticipantNotFoundException when participant not found")
    void checkinParticipant_NotFound() {
        CheckinRequestDTO request = CheckinRequestDTO.builder().participantId(999L).build();
        when(participantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkinService.checkinParticipant(request, currentActor))
                .isInstanceOf(ParticipantNotFoundException.class);
    }

    @Test
    @DisplayName("checkinParticipant - Throws ParticipantAlreadyCheckedInException when duplicate checkin")
    void checkinParticipant_Duplicate() {
        CheckinRequestDTO request = CheckinRequestDTO.builder().participantId(101L).build();
        when(participantRepository.findById(101L)).thenReturn(Optional.of(participant));
        when(checkinRepository.existsByParticipantId(101L)).thenReturn(true);

        assertThatThrownBy(() -> checkinService.checkinParticipant(request, currentActor))
                .isInstanceOf(ParticipantAlreadyCheckedInException.class);
    }

    @Test
    @DisplayName("checkinParticipant - Throws IllegalArgumentException when registration not APPROVED")
    void checkinParticipant_NotApprovedRegistration() {
        registration.setStatus(RegistrationStatus.PENDING);
        CheckinRequestDTO request = CheckinRequestDTO.builder().participantId(101L).build();
        when(participantRepository.findById(101L)).thenReturn(Optional.of(participant));

        assertThatThrownBy(() -> checkinService.checkinParticipant(request, currentActor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not APPROVED");
    }

    @Test
    @DisplayName("checkoutParticipant - Success")
    void checkoutParticipant_Success() {
        CheckoutRequestDTO request = CheckoutRequestDTO.builder().remarks("Left event").build();
        when(checkinRepository.findById(501L)).thenReturn(Optional.of(checkin));
        when(checkinRepository.save(any(Checkin.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckinResponseDTO response = checkinService.checkoutParticipant(501L, request, currentActor);

        assertThat(response.getStatus()).isEqualTo(CheckinStatus.CHECKED_OUT);
        assertThat(response.getCheckedOutBy()).isNotNull();
        assertThat(response.getCheckedOutAt()).isNotNull();
    }

    @Test
    @DisplayName("checkoutParticipant - Throws InvalidCheckinStatusException when already checked out")
    void checkoutParticipant_AlreadyCheckedOut() {
        checkin.setStatus(CheckinStatus.CHECKED_OUT);
        when(checkinRepository.findById(501L)).thenReturn(Optional.of(checkin));

        assertThatThrownBy(() -> checkinService.checkoutParticipant(501L, null, currentActor))
                .isInstanceOf(InvalidCheckinStatusException.class);
    }

    @Test
    @DisplayName("getCheckinById - Success")
    void getCheckinById_Success() {
        when(checkinRepository.findById(501L)).thenReturn(Optional.of(checkin));

        CheckinResponseDTO response = checkinService.getCheckinById(501L);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(501L);
    }

    @Test
    @DisplayName("getCheckinById - Throws CheckinNotFoundException")
    void getCheckinById_NotFound() {
        when(checkinRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkinService.getCheckinById(999L))
                .isInstanceOf(CheckinNotFoundException.class);
    }

    @Test
    @DisplayName("deleteCheckin - Success")
    void deleteCheckin_Success() {
        when(checkinRepository.findById(501L)).thenReturn(Optional.of(checkin));

        checkinService.deleteCheckin(501L, currentActor);

        verify(checkinRepository, times(1)).delete(checkin);
    }

    @Test
    @DisplayName("getEventSummary - Success")
    void getEventSummary_Success() {
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));

        ParticipantEvent pe = ParticipantEvent.builder()
                .id(1L)
                .event(event)
                .participant(participant)
                .build();

        when(participantEventRepository.findAll()).thenReturn(List.of(pe));
        when(checkinRepository.countByEventIdAndStatus(5L, CheckinStatus.CHECKED_IN)).thenReturn(1L);
        when(checkinRepository.countByEventIdAndStatus(5L, CheckinStatus.CHECKED_OUT)).thenReturn(0L);
        when(checkinRepository.countByEventIdAndStatus(5L, CheckinStatus.ABSENT)).thenReturn(0L);

        EventCheckinSummaryDTO summary = checkinService.getEventSummary(5L);

        assertThat(summary).isNotNull();
        assertThat(summary.getEventId()).isEqualTo(5L);
        assertThat(summary.getTotalParticipants()).isEqualTo(1L);
        assertThat(summary.getCheckedIn()).isEqualTo(1L);
        assertThat(summary.getAttendancePercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("bulkCheckinByEvent - Success")
    void bulkCheckinByEvent_Success() {
        when(eventRepository.existsById(5L)).thenReturn(true);
        when(participantRepository.findById(101L)).thenReturn(Optional.of(participant));
        when(checkinRepository.existsByParticipantId(101L)).thenReturn(false);
        when(checkinRepository.save(any(Checkin.class))).thenReturn(checkin);

        BulkCheckinRequestDTO request = BulkCheckinRequestDTO.builder()
                .participantIds(List.of(101L))
                .remarks("Bulk Checkin")
                .build();

        BulkCheckinResponseDTO response = checkinService.bulkCheckinByEvent(5L, request, currentActor);

        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailureCount()).isEqualTo(0);
    }
}
