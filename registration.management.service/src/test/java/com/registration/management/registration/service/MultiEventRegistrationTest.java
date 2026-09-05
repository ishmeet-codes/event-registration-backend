package com.registration.management.registration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.User;
import com.registration.management.enums.Gender;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.event.entities.Event;
import com.registration.management.event.repository.EventRepository;
import com.registration.management.participant.dto.ParticipantCreateDTO;
import com.registration.management.participant.entity.Participant;
import com.registration.management.participant.repository.ParticipantEventRepository;
import com.registration.management.participant.repository.ParticipantRepository;
import com.registration.management.registration.dto.RegistrationCreateRequestDTO;
import com.registration.management.registration.dto.RegistrationResponseDTO;
import com.registration.management.registration.entity.Registration;
import com.registration.management.registration.repository.RegistrationEventRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiEventRegistrationTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RegistrationEventRepository registrationEventRepository;

    @Mock
    private ParticipantEventRepository participantEventRepository;

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

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private School school;
    private SchoolStaff staff;
    private User currentUser;

    @BeforeEach
    void setUp() {
        school = com.registration.management.registration.util.RegistrationTestDataFactory.createTestSchool();
        staff = com.registration.management.registration.util.RegistrationTestDataFactory.createTestStaff(school);
        currentUser = com.registration.management.registration.util.RegistrationTestDataFactory.createTestUser();
    }

    @Test
    void createRegistration_multiEvent_shouldCreateOnlyOneRegistrationRecord() {
        when(schoolRepository.findById(school.getId())).thenReturn(Optional.of(school));
        when(schoolStaffRepository.findFirstByUserIdAndSchoolIdAndActiveTrue(currentUser.getId(), school.getId())).thenReturn(Optional.of(staff));

        List<Long> eventIds = new ArrayList<>();
        List<Event> mockEvents = new ArrayList<>();

        for (long i = 1; i <= 13; i++) {
            eventIds.add(i);
            Event e = Event.builder()
                    .id(i)
                    .eventName("Event " + i)
                    .active(true)
                    .maxRegistrations(100)
                    .registrationDeadline(LocalDateTime.now().plusDays(10))
                    .build();
            mockEvents.add(e);
            when(eventRepository.findById(i)).thenReturn(Optional.of(e));
        }

        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> {
            Registration r = inv.getArgument(0);
            r.setId(7L);
            return r;
        });

        when(participantRepository.save(any(Participant.class))).thenAnswer(inv -> {
            Participant p = inv.getArgument(0);
            p.setId((long) (Math.random() * 1000 + 100));
            return p;
        });

        // 5 Students with specific event assignments
        ParticipantCreateDTO p1 = ParticipantCreateDTO.builder()
                .fullName("Student 1")
                .gender(Gender.MALE)
                .dob(LocalDate.of(2011, 1, 1))
                .guardianPhone("9876543210")
                .eventIds(List.of(1L, 2L))
                .build();

        ParticipantCreateDTO p2 = ParticipantCreateDTO.builder()
                .fullName("Student 2")
                .gender(Gender.FEMALE)
                .dob(LocalDate.of(2011, 2, 2))
                .guardianPhone("9876543211")
                .eventIds(List.of(2L, 3L))
                .build();

        ParticipantCreateDTO p3 = ParticipantCreateDTO.builder()
                .fullName("Student 3")
                .gender(Gender.MALE)
                .dob(LocalDate.of(2011, 3, 3))
                .guardianPhone("9876543212")
                .eventIds(List.of(4L))
                .build();

        ParticipantCreateDTO p4 = ParticipantCreateDTO.builder()
                .fullName("Student 4")
                .gender(Gender.FEMALE)
                .dob(LocalDate.of(2011, 4, 4))
                .guardianPhone("9876543213")
                .eventIds(List.of(1L, 5L))
                .build();

        ParticipantCreateDTO p5 = ParticipantCreateDTO.builder()
                .fullName("Student 5")
                .gender(Gender.MALE)
                .dob(LocalDate.of(2011, 5, 5))
                .guardianPhone("9876543214")
                .eventIds(List.of(6L, 7L))
                .build();

        RegistrationCreateRequestDTO request = RegistrationCreateRequestDTO.builder()
                .schoolId(school.getId())
                .eventIds(eventIds)
                .participants(List.of(p1, p2, p3, p4, p5))
                .remarks("13 Events Application")
                .build();

        RegistrationResponseDTO response = registrationService.createRegistration(request, currentUser);

        assertNotNull(response);
        assertEquals(7L, response.getId());
        assertEquals(RegistrationStatus.PENDING, response.getStatus());
        assertEquals(13L, response.getEventCount());
        assertEquals(5L, response.getParticipantCount());

        // VERIFY EXACT INVARIANTS:
        // 1. Only 1 Registration row saved
        verify(registrationRepository, times(1)).save(any(Registration.class));
        // 2. 13 RegistrationEvent join rows saved
        verify(registrationEventRepository, times(13)).save(any());
        // 3. 5 Participant rows saved
        verify(participantRepository, times(5)).save(any(Participant.class));
        // 4. 9 ParticipantEvent join rows saved (2 + 2 + 1 + 2 + 2 = 9)
        verify(participantEventRepository, times(9)).save(any());
    }
}
