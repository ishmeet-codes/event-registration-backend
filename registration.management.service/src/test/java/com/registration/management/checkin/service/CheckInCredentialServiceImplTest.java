package com.registration.management.checkin.service;

import com.registration.management.auth.entities.User;
import com.registration.management.checkin.dto.CredentialResponseDTO;
import com.registration.management.checkin.entity.CheckInCredential;
import com.registration.management.checkin.enums.CredentialType;
import com.registration.management.checkin.repository.CheckInCredentialRepository;
import com.registration.management.checkin.serviceImpl.CheckInCredentialServiceImpl;
import com.registration.management.event.entities.Event;
import com.registration.management.participant.entity.Participant;
import com.registration.management.registration.entity.Registration;
import com.registration.management.registration.entity.RegistrationEvent;
import com.registration.management.school.entity.School;
import com.registration.management.school.entity.SchoolStaff;
import com.registration.management.school.repository.schoolStaffRepository;
import com.registration.management.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInCredentialServiceImplTest {

    @Mock
    private CheckInCredentialRepository credentialRepository;

    @Mock
    private schoolStaffRepository staffRepository;

    @Mock
    private QrCodeService qrCodeService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private CheckInCredentialServiceImpl credentialService;

    private Registration sampleRegistration;
    private Participant sampleParticipant;
    private Event sampleEvent;
    private School sampleSchool;
    private SchoolStaff sampleStaff;

    @BeforeEach
    void setUp() {
        sampleSchool = School.builder().id(1L).schoolName("Test School").schoolCode("TS01").build();
        sampleEvent = Event.builder().id(10L).eventName("Codefest 2026").build();
        sampleStaff = SchoolStaff.builder().id(5L).fullName("John Teacher").email("teacher@school.com").school(sampleSchool).build();

        sampleParticipant = Participant.builder().id(100L).fullName("Alice Student").build();

        sampleRegistration = Registration.builder()
                .id(50L)
                .school(sampleSchool)
                .createdByStaff(sampleStaff)
                .participants(Set.of(sampleParticipant))
                .build();

        RegistrationEvent re = RegistrationEvent.builder().registration(sampleRegistration).event(sampleEvent).build();
        sampleRegistration.setRegistrationEvents(Set.of(re));
    }

    @Test
    void testTokenGenerationAndHashing() {
        String rawToken = credentialService.generateOpaqueToken();
        assertNotNull(rawToken);
        assertTrue(rawToken.length() >= 32);

        String hash1 = credentialService.hashToken(rawToken);
        String hash2 = credentialService.hashToken(rawToken);
        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    void testCreateCredentialsForApprovedRegistration() {
        when(credentialRepository.findFirstByParticipantIdAndEventIdAndActiveTrue(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(credentialRepository.findFirstBySchoolStaffIdAndEventIdAndCredentialTypeAndActiveTrue(anyLong(), anyLong(), any()))
                .thenReturn(Optional.empty());
        when(credentialRepository.save(any(CheckInCredential.class)))
                .thenAnswer(invocation -> {
                    CheckInCredential c = invocation.getArgument(0);
                    c.setId(new Random().nextLong(1000) + 1);
                    return c;
                });

        List<CheckInCredential> credentials = credentialService.createCredentialsForApprovedRegistration(sampleRegistration);

        assertNotNull(credentials);
        assertFalse(credentials.isEmpty());
        verify(credentialRepository, atLeast(2)).save(any(CheckInCredential.class));
    }

    @Test
    void testRegenerateCredential() {
        CheckInCredential oldCred = CheckInCredential.builder()
                .id(200L)
                .credentialType(CredentialType.PARTICIPANT)
                .tokenHash("oldhash")
                .participant(sampleParticipant)
                .event(sampleEvent)
                .registration(sampleRegistration)
                .active(true)
                .build();

        when(credentialRepository.findById(200L)).thenReturn(Optional.of(oldCred));
        when(credentialRepository.save(any(CheckInCredential.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(qrCodeService.generateQrCodeBase64(anyString(), anyInt(), anyInt()))
                .thenReturn("data:image/png;base64,sample");

        User actor = User.builder().id(1L).email("admin@system.com").build();
        CredentialResponseDTO response = credentialService.regenerateCredential(200L, actor);

        assertNotNull(response);
        assertFalse(oldCred.isActive());
        assertNotNull(response.getToken());
        assertNotNull(response.getQrCodeDataUri());
    }
}
