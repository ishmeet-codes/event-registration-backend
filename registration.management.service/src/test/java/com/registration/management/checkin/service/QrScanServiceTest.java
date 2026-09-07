package com.registration.management.checkin.service;

import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.User;
import com.registration.management.checkin.dto.QrScanRequestDTO;
import com.registration.management.checkin.dto.QrScanResponseDTO;
import com.registration.management.checkin.entity.CheckInCredential;
import com.registration.management.checkin.entity.Checkin;
import com.registration.management.checkin.enums.CredentialType;
import com.registration.management.checkin.repository.CheckInCredentialRepository;
import com.registration.management.checkin.repository.CheckinRepository;
import com.registration.management.checkin.serviceImpl.CheckinServiceImpl;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.event.entities.Event;
import com.registration.management.participant.entity.Participant;
import com.registration.management.registration.entity.Registration;
import com.registration.management.school.entity.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrScanServiceTest {

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private CheckInCredentialRepository credentialRepository;

    @Mock
    private CheckInCredentialService credentialService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private CheckinServiceImpl checkinService;

    private User ocStaffUser;
    private Event sampleEvent;
    private Registration sampleRegistration;
    private Participant sampleParticipant;
    private CheckInCredential validCredential;
    private String sampleRawToken;
    private String sampleTokenHash;

    @BeforeEach
    void setUp() {
        ocStaffUser = User.builder().id(99L).fullName("OC Team Member Rahul").email("rahul.oc@event.com").build();
        sampleEvent = Event.builder().id(42L).eventName("Web Wizards 2026").build();
        School school = School.builder().id(10L).schoolName("ABC High School").build();

        sampleRegistration = Registration.builder()
                .id(101L)
                .status(RegistrationStatus.APPROVED)
                .school(school)
                .build();

        sampleParticipant = Participant.builder()
                .id(202L)
                .fullName("Rohan Kumar")
                .registration(sampleRegistration)
                .build();

        sampleRawToken = "valid-opaque-qr-token-string";
        sampleTokenHash = "hashed-token-string";

        validCredential = CheckInCredential.builder()
                .id(501L)
                .credentialType(CredentialType.PARTICIPANT)
                .tokenHash(sampleTokenHash)
                .participant(sampleParticipant)
                .registration(sampleRegistration)
                .event(sampleEvent)
                .active(true)
                .build();
    }

    @Test
    void testProcessQrScan_Success() {
        QrScanRequestDTO request = QrScanRequestDTO.builder().token(sampleRawToken).eventId(42L).build();

        when(credentialService.hashToken(sampleRawToken)).thenReturn(sampleTokenHash);
        when(credentialRepository.findByTokenHash(sampleTokenHash)).thenReturn(Optional.of(validCredential));
        when(checkinRepository.existsByParticipantId(202L)).thenReturn(false);
        when(checkinRepository.save(any(Checkin.class))).thenAnswer(inv -> {
            Checkin c = inv.getArgument(0);
            c.setId(9821L);
            return c;
        });

        QrScanResponseDTO response = checkinService.processQrScan(request, ocStaffUser);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("SUCCESS", response.getCode());
        assertEquals(9821L, response.getCheckInId());
        assertEquals("Rohan Kumar", response.getPersonName());
        assertEquals("PARTICIPANT", response.getPersonType());
        assertEquals("ABC High School", response.getSchoolName());
        assertEquals("Web Wizards 2026", response.getEventName());
        assertEquals("QR", response.getCheckInMethod());

        // Verify OC Team Member details in response
        assertNotNull(response.getCheckedInBy());
        assertEquals(99L, response.getCheckedInBy().getId());
        assertEquals("OC Team Member Rahul", response.getCheckedInBy().getName());
    }

    @Test
    void testProcessQrScan_WrongEvent() {
        QrScanRequestDTO request = QrScanRequestDTO.builder().token(sampleRawToken).eventId(999L).build(); // Different event ID

        when(credentialService.hashToken(sampleRawToken)).thenReturn(sampleTokenHash);
        when(credentialRepository.findByTokenHash(sampleTokenHash)).thenReturn(Optional.of(validCredential));

        QrScanResponseDTO response = checkinService.processQrScan(request, ocStaffUser);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("WRONG_EVENT", response.getCode());
    }

    @Test
    void testProcessQrScan_DuplicateCheckin() {
        QrScanRequestDTO request = QrScanRequestDTO.builder().token(sampleRawToken).eventId(42L).build();

        when(credentialService.hashToken(sampleRawToken)).thenReturn(sampleTokenHash);
        when(credentialRepository.findByTokenHash(sampleTokenHash)).thenReturn(Optional.of(validCredential));
        when(checkinRepository.existsByParticipantId(202L)).thenReturn(true); // Already checked in

        QrScanResponseDTO response = checkinService.processQrScan(request, ocStaffUser);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("ALREADY_CHECKED_IN", response.getCode());
    }

    @Test
    void testProcessQrScan_RevokedCredential() {
        validCredential.setActive(false);
        QrScanRequestDTO request = QrScanRequestDTO.builder().token(sampleRawToken).eventId(42L).build();

        when(credentialService.hashToken(sampleRawToken)).thenReturn(sampleTokenHash);
        when(credentialRepository.findByTokenHash(sampleTokenHash)).thenReturn(Optional.of(validCredential));

        QrScanResponseDTO response = checkinService.processQrScan(request, ocStaffUser);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("INVALID_QR", response.getCode());
    }
}
