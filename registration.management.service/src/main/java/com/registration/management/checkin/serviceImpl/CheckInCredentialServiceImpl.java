package com.registration.management.checkin.serviceImpl;

import com.registration.management.audit.entities.AuditLog;
import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.User;
import com.registration.management.checkin.dto.CredentialResponseDTO;
import com.registration.management.checkin.entity.CheckInCredential;
import com.registration.management.checkin.enums.CredentialType;
import com.registration.management.checkin.repository.CheckInCredentialRepository;
import com.registration.management.checkin.service.CheckInCredentialService;
import com.registration.management.checkin.service.QrCodeService;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
import com.registration.management.enums.StaffRole;
import com.registration.management.event.entities.Event;
import com.registration.management.participant.entity.Participant;
import com.registration.management.participant.entity.ParticipantEvent;
import com.registration.management.registration.entity.Registration;
import com.registration.management.registration.entity.RegistrationEvent;
import com.registration.management.school.entity.SchoolStaff;
import com.registration.management.school.repository.schoolStaffRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

@Service
@Transactional
public class CheckInCredentialServiceImpl implements CheckInCredentialService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HEX_CHARS = "0123456789abcdef";

    @Autowired
    private CheckInCredentialRepository credentialRepository;

    @Autowired
    private schoolStaffRepository staffRepository;

    @Autowired
    private com.registration.management.participant.repository.ParticipantRepository participantRepository;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public List<CheckInCredential> createCredentialsForApprovedRegistration(Registration registration) {
        List<CheckInCredential> createdCredentials = new ArrayList<>();
        if (registration == null || registration.getId() == null) {
            return createdCredentials;
        }

        // 1. Participant Credentials
        if (registration.getParticipants() != null) {
            for (Participant participant : registration.getParticipants()) {
                Set<Event> participantEvents = new HashSet<>();
                if (participant.getParticipantEvents() != null && !participant.getParticipantEvents().isEmpty()) {
                    for (ParticipantEvent pe : participant.getParticipantEvents()) {
                        if (pe.getEvent() != null) {
                            participantEvents.add(pe.getEvent());
                        }
                    }
                } else if (registration.getRegistrationEvents() != null) {
                    for (RegistrationEvent re : registration.getRegistrationEvents()) {
                        if (re.getEvent() != null) {
                            participantEvents.add(re.getEvent());
                        }
                    }
                }

                for (Event event : participantEvents) {
                    Optional<CheckInCredential> existing = credentialRepository
                            .findFirstByParticipantIdAndEventIdAndActiveTrue(participant.getId(), event.getId());
                    if (existing.isPresent()) {
                        createdCredentials.add(existing.get());
                    } else {
                        String rawToken = generateOpaqueToken();
                        String tokenHash = hashToken(rawToken);

                        CheckInCredential cred = CheckInCredential.builder()
                                .credentialType(CredentialType.PARTICIPANT)
                                .tokenHash(tokenHash)
                                .participant(participant)
                                .registration(registration)
                                .event(event)
                                .active(true)
                                .build();

                        CheckInCredential saved = credentialRepository.save(cred);
                        createdCredentials.add(saved);
                        auditCredentialEvent(registration.getCreatedBy(), saved.getId(), "QR_CREDENTIAL_CREATED",
                                "Participant credential created for ID " + participant.getId() + ", Event " + event.getId());
                    }
                }
            }
        }

        // 2. Login Teacher Credential
        SchoolStaff loginTeacher = registration.getCreatedByStaff();
        if (loginTeacher != null && registration.getRegistrationEvents() != null) {
            for (RegistrationEvent re : registration.getRegistrationEvents()) {
                Event event = re.getEvent();
                if (event != null) {
                    Optional<CheckInCredential> existing = credentialRepository
                            .findFirstBySchoolStaffIdAndEventIdAndCredentialTypeAndActiveTrue(
                                    loginTeacher.getId(), event.getId(), CredentialType.LOGIN_TEACHER);
                    if (existing.isPresent()) {
                        createdCredentials.add(existing.get());
                    } else {
                        String rawToken = generateOpaqueToken();
                        String tokenHash = hashToken(rawToken);

                        CheckInCredential cred = CheckInCredential.builder()
                                .credentialType(CredentialType.LOGIN_TEACHER)
                                .tokenHash(tokenHash)
                                .schoolStaff(loginTeacher)
                                .registration(registration)
                                .event(event)
                                .active(true)
                                .build();

                        CheckInCredential saved = credentialRepository.save(cred);
                        createdCredentials.add(saved);
                        auditCredentialEvent(registration.getCreatedBy(), saved.getId(), "QR_CREDENTIAL_CREATED",
                                "Login Teacher credential created for Staff ID " + loginTeacher.getId() + ", Event " + event.getId());
                    }
                }
            }
        }

        // 3. Accompanying Teachers Credentials
        if (registration.getSchool() != null && registration.getRegistrationEvents() != null) {
            List<SchoolStaff> accompanyingStaff = staffRepository.findAll((root, query, cb) ->
                    cb.and(
                            cb.equal(root.get("school").get("id"), registration.getSchool().getId()),
                            cb.equal(root.get("staffRole"), StaffRole.ACCOMPANYING_TEACHER),
                            cb.equal(root.get("active"), true)
                    )
            );

            for (SchoolStaff teacher : accompanyingStaff) {
                for (RegistrationEvent re : registration.getRegistrationEvents()) {
                    Event event = re.getEvent();
                    if (event != null) {
                        Optional<CheckInCredential> existing = credentialRepository
                                .findFirstBySchoolStaffIdAndEventIdAndCredentialTypeAndActiveTrue(
                                        teacher.getId(), event.getId(), CredentialType.ACCOMPANYING_TEACHER);
                        if (existing.isPresent()) {
                            createdCredentials.add(existing.get());
                        } else {
                            String rawToken = generateOpaqueToken();
                            String tokenHash = hashToken(rawToken);

                            CheckInCredential cred = CheckInCredential.builder()
                                    .credentialType(CredentialType.ACCOMPANYING_TEACHER)
                                    .tokenHash(tokenHash)
                                    .schoolStaff(teacher)
                                    .registration(registration)
                                    .event(event)
                                    .active(true)
                                    .build();

                            CheckInCredential saved = credentialRepository.save(cred);
                            createdCredentials.add(saved);
                            auditCredentialEvent(registration.getCreatedBy(), saved.getId(), "QR_CREDENTIAL_CREATED",
                                    "Accompanying Teacher credential created for Staff ID " + teacher.getId() + ", Event " + event.getId());
                        }
                    }
                }
            }
        }

        return createdCredentials;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CredentialResponseDTO> getMyActiveCredentials(User currentUser) {
        if (currentUser == null) {
            return Collections.emptyList();
        }

        List<CheckInCredential> credentials = new ArrayList<>();

        // 1. Search by school staff if user is attached to staff
        Optional<SchoolStaff> staffOpt = staffRepository.findFirstByUserIdAndActiveTrue(currentUser.getId());
        if (staffOpt.isPresent()) {
            credentials.addAll(credentialRepository.findBySchoolStaffIdAndActiveTrue(staffOpt.get().getId()));
        }

        // 2. Search by participant email if currentUser is a participant
        if (currentUser.getEmail() != null && !currentUser.getEmail().isBlank()) {
            List<Participant> participants = participantRepository.findByEmail(currentUser.getEmail());
            for (Participant p : participants) {
                credentials.addAll(credentialRepository.findByParticipantIdAndActiveTrue(p.getId()));
            }
        }

        // Return formatted DTOs
        List<CredentialResponseDTO> dtos = new ArrayList<>();
        for (CheckInCredential cred : credentials) {
            dtos.add(toDto(cred, null));
        }
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CredentialResponseDTO> getCredentialsByRegistrationId(Long registrationId) {
        List<CheckInCredential> credentials = credentialRepository.findByRegistrationIdAndActiveTrue(registrationId);
        List<CredentialResponseDTO> dtos = new ArrayList<>();
        for (CheckInCredential cred : credentials) {
            dtos.add(toDto(cred, null));
        }
        return dtos;
    }

    @Override
    public CredentialResponseDTO regenerateCredential(Long credentialId, User currentUser) {
        CheckInCredential oldCredential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new EntityNotFoundException("Credential not found with ID: " + credentialId));

        // Revoke old credential
        oldCredential.setActive(false);
        credentialRepository.save(oldCredential);
        auditCredentialEvent(currentUser, oldCredential.getId(), "QR_CREDENTIAL_REVOKED", "Credential revoked during regeneration");

        // Create new credential
        String newRawToken = generateOpaqueToken();
        String newTokenHash = hashToken(newRawToken);

        CheckInCredential newCredential = CheckInCredential.builder()
                .credentialType(oldCredential.getCredentialType())
                .tokenHash(newTokenHash)
                .participant(oldCredential.getParticipant())
                .schoolStaff(oldCredential.getSchoolStaff())
                .registration(oldCredential.getRegistration())
                .event(oldCredential.getEvent())
                .active(true)
                .expiresAt(oldCredential.getExpiresAt())
                .build();

        CheckInCredential saved = credentialRepository.save(newCredential);
        auditCredentialEvent(currentUser, saved.getId(), "QR_CREDENTIAL_REGENERATED", "New credential generated for entity");

        return toDto(saved, newRawToken);
    }

    @Override
    public void revokeCredentialsForRegistration(Long registrationId) {
        List<CheckInCredential> credentials = credentialRepository.findByRegistrationIdAndActiveTrue(registrationId);
        for (CheckInCredential cred : credentials) {
            cred.setActive(false);
            credentialRepository.save(cred);
            auditCredentialEvent(null, cred.getId(), "QR_CREDENTIAL_REVOKED", "Credential revoked due to registration status update");
        }
    }

    @Override
    public String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(HEX_CHARS.charAt((b >> 4) & 0x0f));
            sb.append(HEX_CHARS.charAt(b & 0x0f));
        }
        return UUID.randomUUID().toString().replace("-", "") + sb.toString();
    }

    @Override
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Override
    public CredentialResponseDTO toDto(CheckInCredential credential, String rawToken) {
        if (credential == null) return null;

        String qrData = null;
        if (rawToken != null && !rawToken.isBlank()) {
            qrData = qrCodeService.generateQrCodeBase64(rawToken, 250, 250);
        }

        CredentialResponseDTO.CredentialResponseDTOBuilder builder = CredentialResponseDTO.builder()
                .id(credential.getId())
                .credentialType(credential.getCredentialType())
                .token(rawToken)
                .qrCodeDataUri(qrData)
                .registrationId(credential.getRegistration() != null ? credential.getRegistration().getId() : null)
                .eventId(credential.getEvent() != null ? credential.getEvent().getId() : null)
                .eventName(credential.getEvent() != null ? credential.getEvent().getEventName() : null)
                .active(credential.isActive())
                .expiresAt(credential.getExpiresAt())
                .createdAt(credential.getCreatedAt());

        if (credential.getParticipant() != null) {
            builder.participantId(credential.getParticipant().getId());
            builder.participantName(credential.getParticipant().getFullName());
        }

        if (credential.getSchoolStaff() != null) {
            builder.schoolStaffId(credential.getSchoolStaff().getId());
            builder.schoolStaffName(credential.getSchoolStaff().getFullName());
            if (credential.getSchoolStaff().getSchool() != null) {
                builder.schoolId(credential.getSchoolStaff().getSchool().getId());
                builder.schoolName(credential.getSchoolStaff().getSchool().getSchoolName());
            }
        }

        if (credential.getRegistration() != null && credential.getRegistration().getSchool() != null) {
            builder.schoolId(credential.getRegistration().getSchool().getId());
            builder.schoolName(credential.getRegistration().getSchool().getSchoolName());
        }

        return builder.build();
    }

    private void auditCredentialEvent(User actor, Long credentialId, String eventType, String detail) {
        try {
            AuditLog log = AuditLog.builder()
                    .user(actor)
                    .entityName("CheckInCredential")
                    .entityId(credentialId)
                    .action(AuditAction.UPDATE)
                    .status(AuditStatus.SUCCESS)
                    .newValue("{\"event\":\"" + eventType + "\",\"detail\":\"" + detail + "\"}")
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Ignore non-fatal audit failures
        }
    }
}
