package com.registration.management.registration.service;

import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.common.exception.ResourceNotFoundException;
import com.registration.management.common.exception.SchoolNotActiveException;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.event.entities.Event;
import com.registration.management.event.repository.EventRepository;
import com.registration.management.registration.dto.*;
import com.registration.management.registration.entity.Registration;
import com.registration.management.registration.exception.RegistrationConflictException;
import com.registration.management.registration.exception.RegistrationHasParticipantsException;
import com.registration.management.registration.exception.RegistrationNotFoundException;
import com.registration.management.registration.repository.ParticipantRepository;
import com.registration.management.registration.repository.RegistrationRepository;
import com.registration.management.registration.serviceImpl.RegistrationServiceImpl;
import com.registration.management.school.entity.School;
import com.registration.management.school.entity.SchoolStaff;
import com.registration.management.school.repository.schoolRepository;
import com.registration.management.school.repository.schoolStaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private schoolRepository schoolRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private schoolStaffRepository schoolStaffRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Spy
    private RegistrationStatusValidator statusValidator = new RegistrationStatusValidator();

    @Mock
    private UserRepository userRepository;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private School school;
    private Event event;
    private SchoolStaff staff;
    private User currentUser;
    private Registration registration;

    @BeforeEach
    void setUp() {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        school = com.registration.management.registration.util.RegistrationTestDataFactory.createTestSchool();
        event = com.registration.management.registration.util.RegistrationTestDataFactory.createTestEvent();
        staff = com.registration.management.registration.util.RegistrationTestDataFactory.createTestStaff(school);
        currentUser = com.registration.management.registration.util.RegistrationTestDataFactory.createTestUser();
        registration = com.registration.management.registration.util.RegistrationTestDataFactory.createRegistrationEntity(school, event, staff, currentUser);
    }

    @Test
    void createRegistration_success_shouldReturnRegistrationResponseDTO() {
        RegistrationCreateRequestDTO request = RegistrationCreateRequestDTO.builder()
                .schoolId(5L)
                .eventId(10L)
                .createdByStaffId(15L)
                .remarks("Registration for APEX 2026")
                .build();

        when(schoolRepository.findById(5L)).thenReturn(Optional.of(school));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(schoolStaffRepository.findById(15L)).thenReturn(Optional.of(staff));
        when(registrationRepository.existsBySchoolIdAndEventId(5L, 10L)).thenReturn(false);
        when(registrationRepository.countByEventId(10L)).thenReturn(5L);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration toSave = invocation.getArgument(0);
            toSave.setId(101L);
            return toSave;
        });

        RegistrationResponseDTO result = registrationService.createRegistration(request, currentUser);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(RegistrationStatus.DRAFT, result.getStatus());
        assertEquals("Registration for APEX 2026", result.getRemarks());
        assertEquals("ABC Public School", result.getSchool().getSchoolName());
        assertEquals("APEX 2026", result.getEvent().getEventName());
        assertEquals("Rahul Sharma", result.getCreatedByStaff().getFullName());
    }

    @Test
    void createRegistration_validation1_schoolNotFound_shouldThrow() {
        RegistrationCreateRequestDTO request = RegistrationCreateRequestDTO.builder()
                .schoolId(999L)
                .eventId(10L)
                .createdByStaffId(15L)
                .build();

        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> registrationService.createRegistration(request, currentUser));
    }

    @Test
    void createRegistration_validation9_schoolNotActive_shouldThrow() {
        school.setActive(false);
        RegistrationCreateRequestDTO request = RegistrationCreateRequestDTO.builder()
                .schoolId(5L)
                .eventId(10L)
                .createdByStaffId(15L)
                .build();

        when(schoolRepository.findById(5L)).thenReturn(Optional.of(school));

        assertThrows(SchoolNotActiveException.class, () -> registrationService.createRegistration(request, currentUser));
    }

    @Test
    void createRegistration_validation2_eventNotFound_shouldThrow() {
        RegistrationCreateRequestDTO request = RegistrationCreateRequestDTO.builder()
                .schoolId(5L)
                .eventId(999L)
                .createdByStaffId(15L)
                .build();

        when(schoolRepository.findById(5L)).thenReturn(Optional.of(school));
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> registrationService.createRegistration(request, currentUser));
    }

    @Test
    void createRegistration_validation5_eventNotActive_shouldThrow() {
        event.setActive(false);
        RegistrationCreateRequestDTO request = RegistrationCreateRequestDTO.builder()
                .schoolId(5L)
                .eventId(10L)
                .createdByStaffId(15L)
                .build();

        when(schoolRepository.findById(5L)).thenReturn(Optional.of(school));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(ResponseStatusException.class, () -> registrationService.createRegistration(request, currentUser));
    }

    @Test
    void createRegistration_validation6_deadlinePassed_shouldThrow() {
        event.setRegistrationDeadline(LocalDateTime.now().minusDays(1));
        RegistrationCreateRequestDTO request = RegistrationCreateRequestDTO.builder()
                .schoolId(5L)
                .eventId(10L)
                .createdByStaffId(15L)
                .build();

        when(schoolRepository.findById(5L)).thenReturn(Optional.of(school));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThrows(ResponseStatusException.class, () -> registrationService.createRegistration(request, currentUser));
    }

    @Test
    void createRegistration_validation3_staffNotFound_shouldThrow() {
        RegistrationCreateRequestDTO request = RegistrationCreateRequestDTO.builder()
                .schoolId(5L)
                .eventId(10L)
                .createdByStaffId(999L)
                .build();

        when(schoolRepository.findById(5L)).thenReturn(Optional.of(school));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(schoolStaffRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> registrationService.createRegistration(request, currentUser));
    }

    @Test
    void createRegistration_validation4_staffNotInSchool_shouldThrow() {
        School otherSchool = School.builder().id(99L).build();
        staff.setSchool(otherSchool);

        RegistrationCreateRequestDTO request = RegistrationCreateRequestDTO.builder()
                .schoolId(5L)
                .eventId(10L)
                .createdByStaffId(15L)
                .build();

        when(schoolRepository.findById(5L)).thenReturn(Optional.of(school));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(schoolStaffRepository.findById(15L)).thenReturn(Optional.of(staff));

        assertThrows(ResponseStatusException.class, () -> registrationService.createRegistration(request, currentUser));
    }

    @Test
    void createRegistration_validation7_duplicateRegistration_shouldThrow() {
        RegistrationCreateRequestDTO request = RegistrationCreateRequestDTO.builder()
                .schoolId(5L)
                .eventId(10L)
                .createdByStaffId(15L)
                .build();

        when(schoolRepository.findById(5L)).thenReturn(Optional.of(school));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(schoolStaffRepository.findById(15L)).thenReturn(Optional.of(staff));
        when(registrationRepository.existsBySchoolIdAndEventId(5L, 10L)).thenReturn(true);

        assertThrows(RegistrationConflictException.class, () -> registrationService.createRegistration(request, currentUser));
    }

    @Test
    void createRegistration_validation8_capacityFull_shouldThrow() {
        event.setMaxRegistrations(10);
        RegistrationCreateRequestDTO request = RegistrationCreateRequestDTO.builder()
                .schoolId(5L)
                .eventId(10L)
                .createdByStaffId(15L)
                .build();

        when(schoolRepository.findById(5L)).thenReturn(Optional.of(school));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(schoolStaffRepository.findById(15L)).thenReturn(Optional.of(staff));
        when(registrationRepository.existsBySchoolIdAndEventId(5L, 10L)).thenReturn(false);
        when(registrationRepository.countByEventId(10L)).thenReturn(10L);

        assertThrows(ResponseStatusException.class, () -> registrationService.createRegistration(request, currentUser));
    }

    @Test
    void getRegistrationById_success() {
        when(registrationRepository.findById(101L)).thenReturn(Optional.of(registration));
        when(participantRepository.countByRegistrationId(101L)).thenReturn(3L);

        RegistrationResponseDTO dto = registrationService.getRegistrationById(101L);

        assertNotNull(dto);
        assertEquals(101L, dto.getId());
        assertEquals(3L, dto.getParticipantCount());
    }

    @Test
    void getRegistrationById_notFound_shouldThrowRegistrationNotFoundException() {
        when(registrationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RegistrationNotFoundException.class, () -> registrationService.getRegistrationById(999L));
    }

    @Test
    void updateRegistration_shouldUpdateRemarksOnly() {
        when(registrationRepository.findById(101L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationUpdateRequestDTO request = RegistrationUpdateRequestDTO.builder()
                .remarks("Updated remarks")
                .build();

        RegistrationResponseDTO result = registrationService.updateRegistration(101L, request, currentUser);

        assertEquals("Updated remarks", result.getRemarks());
    }

    @Test
    void deleteRegistration_whenParticipantsExist_shouldThrowRegistrationHasParticipantsException() {
        when(registrationRepository.findById(101L)).thenReturn(Optional.of(registration));
        when(participantRepository.countByRegistrationId(101L)).thenReturn(5L);

        assertThrows(RegistrationHasParticipantsException.class, () -> registrationService.deleteRegistration(101L, currentUser));
        verify(registrationRepository, never()).delete(any(Registration.class));
    }

    @Test
    void deleteRegistration_whenNoParticipants_shouldDelete() {
        when(registrationRepository.findById(101L)).thenReturn(Optional.of(registration));
        when(participantRepository.countByRegistrationId(101L)).thenReturn(0L);

        assertDoesNotThrow(() -> registrationService.deleteRegistration(101L, currentUser));
        verify(registrationRepository, times(1)).delete(registration);
    }

    @Test
    void submitRegistration_fromDraftToPending_shouldSucceed() {
        registration.setStatus(RegistrationStatus.DRAFT);
        when(registrationRepository.findById(101L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponseDTO result = registrationService.submitRegistration(101L, null, currentUser);

        assertEquals(RegistrationStatus.PENDING, result.getStatus());
    }

    @Test
    void approveRegistration_fromPendingToApproved_shouldSucceed() {
        registration.setStatus(RegistrationStatus.PENDING);
        when(registrationRepository.findById(101L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponseDTO result = registrationService.approveRegistration(101L, currentUser);

        assertEquals(RegistrationStatus.APPROVED, result.getStatus());
    }

    @Test
    void rejectRegistration_fromPendingToRejected_shouldSucceed() {
        registration.setStatus(RegistrationStatus.PENDING);
        when(registrationRepository.findById(101L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationRemarksRequestDTO remarks = RegistrationRemarksRequestDTO.builder().remarks("Incomplete").build();
        RegistrationResponseDTO result = registrationService.rejectRegistration(101L, remarks, currentUser);

        assertEquals(RegistrationStatus.REJECTED, result.getStatus());
        assertEquals("Incomplete", result.getRemarks());
    }

    @Test
    void cancelRegistration_fromPendingToCancelled_shouldSucceed() {
        registration.setStatus(RegistrationStatus.PENDING);
        when(registrationRepository.findById(101L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponseDTO result = registrationService.cancelRegistration(101L, null, currentUser);

        assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
    }

    @Test
    void getRegistrations_paginated_shouldReturnPage() {
        Page<Registration> page = new PageImpl<>(List.of(registration));
        when(registrationRepository.findAll(ArgumentMatchers.<Specification<Registration>>any(), any(Pageable.class))).thenReturn(page);

        Page<RegistrationResponseDTO> result = registrationService.getRegistrations(
                "ABC", List.of(RegistrationStatus.PENDING), 5L, 10L, 15L,
                LocalDate.now().minusDays(5), LocalDate.now(), null, null,
                0, 20, "createdAt,desc"
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getRegistrationStatistics_shouldReturnAggregatedStats() {
        when(registrationRepository.count()).thenReturn(100L);
        when(registrationRepository.countByStatus(RegistrationStatus.DRAFT)).thenReturn(10L);
        when(registrationRepository.countByStatus(RegistrationStatus.PENDING)).thenReturn(20L);
        when(registrationRepository.countByStatus(RegistrationStatus.SUBMITTED)).thenReturn(5L);
        when(registrationRepository.countByStatus(RegistrationStatus.APPROVED)).thenReturn(50L);
        when(registrationRepository.countByStatus(RegistrationStatus.REJECTED)).thenReturn(5L);
        when(registrationRepository.countByStatus(RegistrationStatus.CANCELLED)).thenReturn(5L);
        when(registrationRepository.countByStatus(RegistrationStatus.COMPLETED)).thenReturn(5L);

        RegistrationStatisticsDTO stats = registrationService.getRegistrationStatistics(null, null);

        assertNotNull(stats);
        assertEquals(100L, stats.getTotal());
        assertEquals(10L, stats.getDraft());
        assertEquals(25L, stats.getPending());
        assertEquals(50L, stats.getApproved());
        assertEquals(5L, stats.getRejected());
        assertEquals(5L, stats.getCancelled());
        assertEquals(5L, stats.getCompleted());
    }
}
