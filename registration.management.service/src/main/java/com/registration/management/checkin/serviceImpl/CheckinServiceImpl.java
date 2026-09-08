package com.registration.management.checkin.serviceImpl;

import com.registration.management.audit.entities.AuditLog;
import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.User;
import com.registration.management.checkin.dto.*;
import com.registration.management.checkin.entity.Checkin;
import com.registration.management.checkin.enums.CheckinStatus;
import com.registration.management.checkin.exception.CheckinNotFoundException;
import com.registration.management.checkin.exception.InvalidCheckinStatusException;
import com.registration.management.checkin.exception.ParticipantAlreadyCheckedInException;
import com.registration.management.checkin.repository.CheckinRepository;
import com.registration.management.checkin.service.CheckinService;
import com.registration.management.checkin.specification.CheckinSpecification;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.event.entities.Event;
import com.registration.management.event.repository.EventRepository;
import com.registration.management.participant.entity.Participant;
import com.registration.management.participant.entity.ParticipantEvent;
import com.registration.management.participant.exception.ParticipantNotFoundException;
import com.registration.management.participant.repository.ParticipantEventRepository;
import com.registration.management.participant.repository.ParticipantRepository;
import com.registration.management.registration.entity.Registration;
import com.registration.management.common.exception.ResourceNotFoundException;
import com.registration.management.checkin.entity.CheckInCredential;
import com.registration.management.checkin.enums.CheckInMethod;
import com.registration.management.checkin.repository.CheckInCredentialRepository;
import com.registration.management.checkin.service.CheckInCredentialService;
import com.registration.management.school.entity.School;
import com.registration.management.school.entity.SchoolStaff;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantEventRepository participantEventRepository;
    private final EventRepository eventRepository;
    private final AuditLogRepository auditLogRepository;
    private final CheckInCredentialRepository credentialRepository;
    private final CheckInCredentialService credentialService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    public CheckinResponseDTO checkinParticipant(CheckinRequestDTO request, User currentActor) {
        Participant participant = participantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new ParticipantNotFoundException("Participant not found with id: " + request.getParticipantId()));

        Registration registration = participant.getRegistration();
        if (registration == null) {
            throw new IllegalArgumentException("Participant is not associated with a registration");
        }

        if (registration.getStatus() != RegistrationStatus.APPROVED) {
            throw new IllegalArgumentException("Participant registration is not APPROVED (current status: " + registration.getStatus() + ")");
        }

        if (checkinRepository.existsByParticipantId(participant.getId())) {
            throw new ParticipantAlreadyCheckedInException("Participant has already been checked in.");
        }

        Set<ParticipantEvent> participantEvents = participant.getParticipantEvents();
        if (participantEvents.isEmpty()) {
            throw new IllegalArgumentException("Participant is not assigned to any event.");
        }

        Event event = participantEvents.iterator().next().getEvent();
        validateEventSchedule(event);

        School school = registration.getSchool();

        Checkin checkin = Checkin.builder()
                .participant(participant)
                .event(event)
                .registration(registration)
                .school(school)
                .status(CheckinStatus.CHECKED_IN)
                .checkedInAt(LocalDateTime.now())
                .checkedInBy(currentActor)
                .remarks(request.getRemarks())
                .build();

        Checkin saved = checkinRepository.save(checkin);
        logAudit(currentActor, "Checkin", saved.getId(), AuditAction.CREATE, AuditStatus.SUCCESS, null);

        publishCheckinNotification(saved, com.registration.management.notification.enums.NotificationType.CHECKIN_CONFIRMED, "Participant " + participant.getFullName() + " checked in for event " + event.getEventName());

        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckinResponseDTO getCheckinById(Long id) {
        Checkin checkin = checkinRepository.findById(id)
                .orElseThrow(() -> new CheckinNotFoundException("Check-in record not found with id: " + id));
        return mapToResponseDTO(checkin);
    }

    @Override
    public CheckinResponseDTO correctCheckin(Long id, CheckinRequestDTO request, User currentActor) {
        Checkin checkin = checkinRepository.findById(id)
                .orElseThrow(() -> new CheckinNotFoundException("Check-in record not found with id: " + id));

        validateEventSchedule(checkin.getEvent());

        if (!checkin.getParticipant().getId().equals(request.getParticipantId())) {
            Participant newParticipant = participantRepository.findById(request.getParticipantId())
                    .orElseThrow(() -> new ParticipantNotFoundException("Participant not found with id: " + request.getParticipantId()));

            Registration registration = newParticipant.getRegistration();
            if (registration.getStatus() != RegistrationStatus.APPROVED) {
                throw new IllegalArgumentException("Participant registration is not APPROVED");
            }

            if (checkinRepository.existsByParticipantId(newParticipant.getId())) {
                throw new ParticipantAlreadyCheckedInException("Participant has already been checked in.");
            }

            Set<ParticipantEvent> participantEvents = newParticipant.getParticipantEvents();
            if (participantEvents.isEmpty()) {
                throw new IllegalArgumentException("Participant is not assigned to any event.");
            }

            checkin.setParticipant(newParticipant);
            checkin.setEvent(participantEvents.iterator().next().getEvent());
            checkin.setRegistration(registration);
            checkin.setSchool(registration.getSchool());
        }

        checkin.setRemarks(request.getRemarks());
        checkin.setUpdatedAt(LocalDateTime.now());

        Checkin updated = checkinRepository.save(checkin);
        logAudit(currentActor, "Checkin", updated.getId(), AuditAction.UPDATE, AuditStatus.SUCCESS, null);

        return mapToResponseDTO(updated);
    }

    @Override
    public void deleteCheckin(Long id, User currentActor) {
        Checkin checkin = checkinRepository.findById(id)
                .orElseThrow(() -> new CheckinNotFoundException("Check-in record not found with id: " + id));

        checkinRepository.delete(checkin);
        logAudit(currentActor, "Checkin", id, AuditAction.DELETE, AuditStatus.SUCCESS, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CheckinResponseDTO> getCheckins(
            String search,
            Long eventId,
            Long schoolId,
            Long registrationId,
            CheckinStatus status,
            Long checkedInBy,
            LocalDate date,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    ) {
        Specification<Checkin> spec = CheckinSpecification.buildSpecification(
                search, eventId, schoolId, registrationId, status, checkedInBy, date, dateFrom, dateTo
        );
        return checkinRepository.findAll(spec, pageable).map(this::mapToResponseDTO);
    }

    @Override
    public CheckinResponseDTO updateStatus(Long id, CheckinStatusUpdateRequestDTO request, User currentActor) {
        Checkin checkin = checkinRepository.findById(id)
                .orElseThrow(() -> new CheckinNotFoundException("Check-in record not found with id: " + id));

        if (request.getStatus() == CheckinStatus.CHECKED_IN || request.getStatus() == CheckinStatus.CHECKED_OUT) {
            validateEventSchedule(checkin.getEvent());
        }

        CheckinStatus newStatus = request.getStatus();
        checkin.setStatus(newStatus);
        if (request.getRemarks() != null) {
            checkin.setRemarks(request.getRemarks());
        }

        if (newStatus == CheckinStatus.CHECKED_OUT && checkin.getCheckedOutAt() == null) {
            checkin.setCheckedOutAt(LocalDateTime.now());
            checkin.setCheckedOutBy(currentActor);
        }

        checkin.setUpdatedAt(LocalDateTime.now());
        Checkin saved = checkinRepository.save(checkin);
        logAudit(currentActor, "Checkin", saved.getId(), AuditAction.UPDATE, AuditStatus.SUCCESS, null);

        return mapToResponseDTO(saved);
    }

    @Override
    public CheckinResponseDTO checkoutParticipant(Long id, CheckoutRequestDTO request, User currentActor) {
        Checkin checkin = checkinRepository.findById(id)
                .orElseThrow(() -> new CheckinNotFoundException("Check-in record not found with id: " + id));

        validateEventSchedule(checkin.getEvent());

        if (checkin.getStatus() == CheckinStatus.CHECKED_OUT) {
            throw new InvalidCheckinStatusException("Participant is already checked out.");
        }

        checkin.setStatus(CheckinStatus.CHECKED_OUT);
        checkin.setCheckedOutAt(LocalDateTime.now());
        checkin.setCheckedOutBy(currentActor);

        if (request != null && request.getRemarks() != null && !request.getRemarks().isBlank()) {
            checkin.setRemarks(request.getRemarks());
        }

        checkin.setUpdatedAt(LocalDateTime.now());
        Checkin saved = checkinRepository.save(checkin);
        logAudit(currentActor, "Checkin", saved.getId(), AuditAction.UPDATE, AuditStatus.SUCCESS, null);

        publishCheckinNotification(saved, com.registration.management.notification.enums.NotificationType.CHECKOUT_CONFIRMED, "Participant " + (saved.getParticipant() != null ? saved.getParticipant().getFullName() : "Attendee") + " checked out.");

        return mapToResponseDTO(saved);
    }

    private void publishCheckinNotification(Checkin checkin, com.registration.management.notification.enums.NotificationType type, String message) {
        try {
            Long recipientId = null;
            if (checkin.getRegistration() != null && checkin.getRegistration().getCreatedByStaff() != null && checkin.getRegistration().getCreatedByStaff().getUser() != null) {
                recipientId = checkin.getRegistration().getCreatedByStaff().getUser().getId();
            } else if (checkin.getRegistration() != null && checkin.getRegistration().getCreatedBy() != null) {
                recipientId = checkin.getRegistration().getCreatedBy().getId();
            }

            if (recipientId != null && eventPublisher != null) {
                Map<String, Object> vars = new HashMap<>();
                vars.put("eventName", checkin.getEvent() != null ? checkin.getEvent().getEventName() : "Event");
                vars.put("schoolName", checkin.getSchool() != null ? checkin.getSchool().getSchoolName() : "School");
                vars.put("referenceId", checkin.getId());

                com.registration.management.notification.event.DomainNotificationEvent event = com.registration.management.notification.event.DomainNotificationEvent.builder()
                        .type(type)
                        .recipientUserId(recipientId)
                        .referenceType("CHECK_IN")
                        .referenceId(checkin.getId())
                        .title(type.name().replace("_", " "))
                        .message(message)
                        .variables(vars)
                        .idempotencyKey("CHECKIN_" + type.name() + "_" + checkin.getId())
                        .build();

                eventPublisher.publishEvent(event);
            }
        } catch (Exception ex) {
            // non-blocking
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EventParticipantAttendanceDTO getParticipantAttendance(Long participantId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException("Participant not found with id: " + participantId));

        Optional<Checkin> checkinOpt = checkinRepository.findByParticipantId(participantId);
        if (checkinOpt.isPresent()) {
            return mapToEventParticipantAttendanceDTO(participant, checkinOpt.get());
        } else {
            return mapToEventParticipantAttendanceDTO(participant, null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinResponseDTO> getEventCheckins(Long eventId) {
        return checkinRepository.findByEventId(eventId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinResponseDTO> getRegistrationCheckins(Long registrationId) {
        return checkinRepository.findByRegistrationId(registrationId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinResponseDTO> getSchoolCheckins(Long schoolId) {
        return checkinRepository.findBySchoolId(schoolId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventCheckinSummaryDTO getEventSummary(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        List<ParticipantEvent> pes = participantEventRepository.findAll().stream()
                .filter(pe -> pe.getEvent().getId().equals(eventId)
                        && pe.getParticipant().getRegistration() != null
                        && pe.getParticipant().getRegistration().getStatus() == RegistrationStatus.APPROVED)
                .collect(Collectors.toList());

        long totalParticipants = pes.size();
        long checkedIn = checkinRepository.countByEventIdAndStatus(eventId, CheckinStatus.CHECKED_IN);
        long qrCheckins = checkinRepository.countByEventIdAndCheckInMethod(eventId, CheckInMethod.QR);
        long manualCheckins = checkinRepository.countByEventIdAndCheckInMethod(eventId, CheckInMethod.MANUAL);
        long checkedOut = checkinRepository.countByEventIdAndStatus(eventId, CheckinStatus.CHECKED_OUT);
        long absent = checkinRepository.countByEventIdAndStatus(eventId, CheckinStatus.ABSENT);

        long notCheckedIn = totalParticipants - (checkedIn + checkedOut + absent);
        if (notCheckedIn < 0) {
            notCheckedIn = 0;
        }

        double percentage = totalParticipants > 0
                ? ((double) (checkedIn + checkedOut) / totalParticipants) * 100.0
                : 0.0;

        return EventCheckinSummaryDTO.builder()
                .eventId(event.getId())
                .eventName(event.getEventName())
                .totalParticipants(totalParticipants)
                .checkedIn(checkedIn)
                .qrCheckins(qrCheckins)
                .manualCheckins(manualCheckins)
                .checkedOut(checkedOut)
                .notCheckedIn(notCheckedIn)
                .absent(absent)
                .attendancePercentage(Math.round(percentage * 100.0) / 100.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventParticipantAttendanceDTO> getEventParticipantsAttendance(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }

        List<ParticipantEvent> pes = participantEventRepository.findAll().stream()
                .filter(pe -> pe.getEvent().getId().equals(eventId)
                        && pe.getParticipant().getRegistration() != null
                        && pe.getParticipant().getRegistration().getStatus() == RegistrationStatus.APPROVED)
                .collect(Collectors.toList());

        List<EventParticipantAttendanceDTO> result = new ArrayList<>();
        for (ParticipantEvent pe : pes) {
            Participant participant = pe.getParticipant();
            Optional<Checkin> checkinOpt = checkinRepository.findByParticipantId(participant.getId());
            result.add(mapToEventParticipantAttendanceDTO(participant, checkinOpt.orElse(null)));
        }

        return result;
    }

    @Override
    public BulkCheckinResponseDTO bulkCheckin(BulkCheckinRequestDTO request, User currentActor) {
        List<Long> participantIds = request.getParticipantIds();
        int total = participantIds.size();

        List<CheckinResponseDTO> checkins = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Long pId : participantIds) {
            try {
                CheckinRequestDTO req = CheckinRequestDTO.builder()
                        .participantId(pId)
                        .remarks(request.getRemarks())
                        .build();
                CheckinResponseDTO dto = checkinParticipant(req, currentActor);
                checkins.add(dto);
            } catch (Exception e) {
                errors.add("Participant ID " + pId + ": " + e.getMessage());
            }
        }

        return BulkCheckinResponseDTO.builder()
                .totalSubmitted(total)
                .successCount(checkins.size())
                .failureCount(errors.size())
                .checkins(checkins)
                .errors(errors)
                .build();
    }

    @Override
    public BulkCheckinResponseDTO bulkCheckinByEvent(Long eventId, BulkCheckinRequestDTO request, User currentActor) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }

        List<Long> participantIds = request.getParticipantIds();
        int total = participantIds.size();

        List<CheckinResponseDTO> checkins = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Long pId : participantIds) {
            try {
                Participant participant = participantRepository.findById(pId)
                        .orElseThrow(() -> new ParticipantNotFoundException("Participant not found with id: " + pId));

                boolean belongsToEvent = participant.getParticipantEvents().stream()
                        .anyMatch(pe -> pe.getEvent().getId().equals(eventId));

                if (!belongsToEvent) {
                    throw new IllegalArgumentException("Participant does not belong to event ID: " + eventId);
                }

                CheckinRequestDTO req = CheckinRequestDTO.builder()
                        .participantId(pId)
                        .remarks(request.getRemarks())
                        .build();

                CheckinResponseDTO dto = checkinParticipant(req, currentActor);
                checkins.add(dto);
            } catch (Exception e) {
                errors.add("Participant ID " + pId + ": " + e.getMessage());
            }
        }

        return BulkCheckinResponseDTO.builder()
                .totalSubmitted(total)
                .successCount(checkins.size())
                .failureCount(errors.size())
                .checkins(checkins)
                .errors(errors)
                .build();
    }

    private CheckinResponseDTO mapToResponseDTO(Checkin checkin) {
        Participant participant = checkin.getParticipant();
        School school = checkin.getSchool();
        Event event = checkin.getEvent();
        Registration registration = checkin.getRegistration();

        CheckinResponseDTO.ParticipantSummary participantSummary = CheckinResponseDTO.ParticipantSummary.builder()
                .id(participant != null ? participant.getId() : null)
                .name(participant != null ? participant.getFullName() : null)
                .className(participant != null ? participant.getClassName() : null)
                .build();

        CheckinResponseDTO.SchoolSummary schoolSummary = CheckinResponseDTO.SchoolSummary.builder()
                .id(school != null ? school.getId() : null)
                .name(school != null ? school.getSchoolName() : null)
                .code(school != null ? school.getSchoolCode() : null)
                .build();

        CheckinResponseDTO.EventSummary eventSummary = CheckinResponseDTO.EventSummary.builder()
                .id(event != null ? event.getId() : null)
                .name(event != null ? event.getEventName() : null)
                .build();

        CheckinResponseDTO.RegistrationSummary registrationSummary = CheckinResponseDTO.RegistrationSummary.builder()
                .id(registration != null ? registration.getId() : null)
                .build();

        User checkedInBy = checkin.getCheckedInBy();
        CheckinResponseDTO.UserSummary checkedInBySummary = checkedInBy != null
                ? CheckinResponseDTO.UserSummary.builder()
                .id(checkedInBy.getId())
                .name(checkedInBy.getFullName() != null ? checkedInBy.getFullName() : checkedInBy.getEmail())
                .build()
                : null;

        User checkedOutBy = checkin.getCheckedOutBy();
        CheckinResponseDTO.UserSummary checkedOutBySummary = checkedOutBy != null
                ? CheckinResponseDTO.UserSummary.builder()
                .id(checkedOutBy.getId())
                .name(checkedOutBy.getFullName() != null ? checkedOutBy.getFullName() : checkedOutBy.getEmail())
                .build()
                : null;

        return CheckinResponseDTO.builder()
                .id(checkin.getId())
                .participant(participantSummary)
                .school(schoolSummary)
                .event(eventSummary)
                .registration(registrationSummary)
                .status(checkin.getStatus())
                .checkedInAt(checkin.getCheckedInAt())
                .checkedInBy(checkedInBySummary)
                .checkedOutAt(checkin.getCheckedOutAt())
                .checkedOutBy(checkedOutBySummary)
                .remarks(checkin.getRemarks())
                .build();
    }

    private EventParticipantAttendanceDTO mapToEventParticipantAttendanceDTO(Participant participant, Checkin checkin) {
        Registration reg = participant.getRegistration();
        School school = reg != null ? reg.getSchool() : null;

        Long checkedInById = null;
        String checkedInByName = null;
        if (checkin != null && checkin.getCheckedInBy() != null) {
            User u = checkin.getCheckedInBy();
            checkedInById = u.getId();
            checkedInByName = u.getFullName() != null ? u.getFullName() : u.getEmail();
        }

        Long checkedOutById = null;
        String checkedOutByName = null;
        if (checkin != null && checkin.getCheckedOutBy() != null) {
            User u = checkin.getCheckedOutBy();
            checkedOutById = u.getId();
            checkedOutByName = u.getFullName() != null ? u.getFullName() : u.getEmail();
        }

        return EventParticipantAttendanceDTO.builder()
                .participantId(participant.getId())
                .participantName(participant.getFullName())
                .className(participant.getClassName())
                .schoolId(school != null ? school.getId() : null)
                .schoolName(school != null ? school.getSchoolName() : null)
                .schoolCode(school != null ? school.getSchoolCode() : null)
                .registrationId(reg != null ? reg.getId() : null)
                .checkinId(checkin != null ? checkin.getId() : null)
                .status(checkin != null ? checkin.getStatus() : CheckinStatus.NOT_CHECKED_IN)
                .checkedInAt(checkin != null ? checkin.getCheckedInAt() : null)
                .checkedOutAt(checkin != null ? checkin.getCheckedOutAt() : null)
                .checkedInById(checkedInById)
                .checkedInBy(checkedInByName)
                .checkedOutById(checkedOutById)
                .checkedOutBy(checkedOutByName)
                .remarks(checkin != null ? checkin.getRemarks() : null)
                .build();
    }

    private void validateEventSchedule(Event event) {
        if (event == null) return;

        LocalDate eventDate = event.getEventDate();
        if (eventDate == null) return;

        LocalTime startTime = event.getStartTime();
        LocalTime endTime = event.getEndTime();

        LocalDateTime startDateTime = startTime != null
                ? LocalDateTime.of(eventDate, startTime)
                : eventDate.atStartOfDay();

        LocalDateTime endDateTime = endTime != null
                ? LocalDateTime.of(eventDate, endTime)
                : eventDate.atTime(LocalTime.MAX);

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(startDateTime)) {
            String startStr = startTime != null ? startTime.toString() : "00:00";
            throw new InvalidCheckinStatusException(
                    "Check-in / Check-out not allowed. Event '" + event.getEventName() +
                    "' is scheduled for " + eventDate + " starting at " + startStr +
                    " (Current time: " + now.toLocalDate() + " " + String.format("%02d:%02d", now.getHour(), now.getMinute()) + ")"
            );
        }

        if (now.isAfter(endDateTime)) {
            String endStr = endTime != null ? endTime.toString() : "23:59";
            throw new InvalidCheckinStatusException(
                    "Check-in / Check-out not allowed. Event '" + event.getEventName() +
                    "' ended on " + eventDate + " at " + endStr +
                    " (Current time: " + now.toLocalDate() + " " + String.format("%02d:%02d", now.getHour(), now.getMinute()) + ")"
            );
        }
    }

    private void logAudit(User user, String entityName, Long entityId, AuditAction action, AuditStatus status, String errorMessage) {
        try {
            AuditLog log = AuditLog.builder()
                    .user(user)
                    .entityName(entityName)
                    .entityId(entityId)
                    .action(action)
                    .status(status)
                    .errorMessage(errorMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(log);
        } catch (Exception ignored) {
        }
    }

    @Override
    public QrScanResponseDTO processQrScan(QrScanRequestDTO request, User scannerUser) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            logAudit(scannerUser, "CheckinScan", null, AuditAction.CREATE, AuditStatus.FAILED, "Empty QR token submitted");
            return QrScanResponseDTO.builder()
                    .success(false)
                    .code("INVALID_QR")
                    .message("Invalid or inactive QR credential.")
                    .build();
        }

        String tokenHash = credentialService.hashToken(request.getToken().trim());
        Optional<CheckInCredential> credentialOpt = credentialRepository.findByTokenHash(tokenHash);

        if (credentialOpt.isEmpty()) {
            logAudit(scannerUser, "CheckinScan", null, AuditAction.CREATE, AuditStatus.FAILED, "INVALID_QR_SCAN: Token hash not found");
            return QrScanResponseDTO.builder()
                    .success(false)
                    .code("INVALID_QR")
                    .message("Invalid or inactive QR credential.")
                    .build();
        }

        CheckInCredential credential = credentialOpt.get();

        if (!credential.isActive()) {
            logAudit(scannerUser, "CheckInCredential", credential.getId(), AuditAction.CREATE, AuditStatus.FAILED, "INVALID_QR_SCAN: Credential revoked/inactive");
            return QrScanResponseDTO.builder()
                    .success(false)
                    .code("INVALID_QR")
                    .message("Invalid or inactive QR credential.")
                    .build();
        }

        if (credential.getExpiresAt() != null && LocalDateTime.now().isAfter(credential.getExpiresAt())) {
            logAudit(scannerUser, "CheckInCredential", credential.getId(), AuditAction.CREATE, AuditStatus.FAILED, "INVALID_QR_SCAN: Credential expired at " + credential.getExpiresAt());
            return QrScanResponseDTO.builder()
                    .success(false)
                    .code("EXPIRED_QR")
                    .message("This QR credential has expired.")
                    .build();
        }

        Event event = credential.getEvent();
        if (request.getEventId() != null && !event.getId().equals(request.getEventId())) {
            logAudit(scannerUser, "CheckInCredential", credential.getId(), AuditAction.CREATE, AuditStatus.FAILED,
                    "WRONG_EVENT_QR_SCAN: Credential event ID " + event.getId() + " does not match scanned event ID " + request.getEventId());
            return QrScanResponseDTO.builder()
                    .success(false)
                    .code("WRONG_EVENT")
                    .message("This QR credential is not valid for this event.")
                    .build();
        }

        Registration registration = credential.getRegistration();
        if (registration == null || registration.getStatus() != RegistrationStatus.APPROVED) {
            String statusStr = registration != null ? registration.getStatus().name() : "NULL";
            logAudit(scannerUser, "CheckInCredential", credential.getId(), AuditAction.CREATE, AuditStatus.FAILED, "UNAPPROVED_REGISTRATION: Status " + statusStr);
            return QrScanResponseDTO.builder()
                    .success(false)
                    .code("UNAPPROVED_REGISTRATION")
                    .message("Registration is not approved.")
                    .build();
        }

        // Validate event schedule
        validateEventSchedule(event);

        Participant participant = credential.getParticipant();
        SchoolStaff staff = credential.getSchoolStaff();

        // Check duplicate check-in
        if (participant != null && checkinRepository.existsByParticipantId(participant.getId())) {
            logAudit(scannerUser, "Checkin", null, AuditAction.CREATE, AuditStatus.FAILED, "DUPLICATE_QR_CHECKIN: Participant ID " + participant.getId());
            return QrScanResponseDTO.builder()
                    .success(false)
                    .code("ALREADY_CHECKED_IN")
                    .message("This person has already been checked in.")
                    .build();
        }

        if (staff != null && checkinRepository.existsBySchoolStaffIdAndEventId(staff.getId(), event.getId())) {
            logAudit(scannerUser, "Checkin", null, AuditAction.CREATE, AuditStatus.FAILED, "DUPLICATE_QR_CHECKIN: Staff ID " + staff.getId() + " for event " + event.getId());
            return QrScanResponseDTO.builder()
                    .success(false)
                    .code("ALREADY_CHECKED_IN")
                    .message("This person has already been checked in.")
                    .build();
        }

        School school = registration.getSchool();

        Checkin checkin = Checkin.builder()
                .participant(participant)
                .schoolStaff(staff)
                .event(event)
                .registration(registration)
                .school(school)
                .status(CheckinStatus.CHECKED_IN)
                .checkInMethod(CheckInMethod.QR)
                .checkedInAt(LocalDateTime.now())
                .checkedInBy(scannerUser)
                .remarks("QR Code Check-in")
                .build();

        Checkin saved = checkinRepository.save(checkin);

        String personName = participant != null ? participant.getFullName() : (staff != null ? staff.getFullName() : "N/A");
        String personType = credential.getCredentialType().name();
        String schoolName = school != null ? school.getSchoolName() : "N/A";
        String eventName = event != null ? event.getEventName() : "N/A";

        QrScanResponseDTO.CheckedInByDTO checkedInByDTO = scannerUser != null ? QrScanResponseDTO.CheckedInByDTO.builder()
                .id(scannerUser.getId())
                .name(scannerUser.getFullName() != null ? scannerUser.getFullName() : scannerUser.getEmail())
                .email(scannerUser.getEmail())
                .build() : null;

        logAudit(scannerUser, "Checkin", saved.getId(), AuditAction.CREATE, AuditStatus.SUCCESS,
                "QR_CHECKIN_SUCCESS: Credential " + credential.getId() + ", Person " + personName);

        return QrScanResponseDTO.builder()
                .success(true)
                .code("SUCCESS")
                .message("Check-in successful")
                .checkInId(saved.getId())
                .personName(personName)
                .personType(personType)
                .schoolName(schoolName)
                .eventName(eventName)
                .checkInMethod("QR")
                .checkedInAt(saved.getCheckedInAt())
                .checkedInBy(checkedInByDTO)
                .build();
    }
}
