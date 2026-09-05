package com.registration.management.registration.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.registration.management.audit.entities.AuditLog;
import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.common.exception.ResourceNotFoundException;
import com.registration.management.common.exception.SchoolNotActiveException;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
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
import com.registration.management.registration.service.RegistrationService;
import com.registration.management.registration.service.RegistrationStatusValidator;
import com.registration.management.school.entity.School;
import com.registration.management.school.entity.SchoolStaff;
import com.registration.management.school.repository.schoolRepository;
import com.registration.management.school.repository.schoolStaffRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final schoolRepository schoolRepository;
    private final EventRepository eventRepository;
    private final schoolStaffRepository schoolStaffRepository;
    private final ParticipantRepository participantRepository;
    private final RegistrationStatusValidator statusValidator;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    @Transactional(readOnly = true)
    public RegistrationResponseDTO getRegistrationById(Long id) {
        Registration registration = findRegistration(id);
        return toDto(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistrationResponseDTO> getRegistrations(
            String search,
            List<RegistrationStatus> statuses,
            Long schoolId,
            Long eventId,
            Long createdByStaffId,
            LocalDate createdFrom,
            LocalDate createdTo,
            LocalDate eventDateFrom,
            LocalDate eventDateTo,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = createPageable(page, size, sort);
        Specification<Registration> spec = buildSpecification(
                search, statuses, schoolId, eventId, createdByStaffId, createdFrom, createdTo, eventDateFrom, eventDateTo
        );
        return registrationRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Override
    public RegistrationResponseDTO createRegistration(RegistrationCreateRequestDTO request, User currentUser) {
        final User resolvedUser = resolveCurrentUser(currentUser);

        // Validation 1 — School exists
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + request.getSchoolId()));

        // Validation 9 — School active
        if (!school.isActive()) {
            throw new SchoolNotActiveException("School is not active");
        }

        // Validation 2 — Event exists
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.getEventId()));

        // Validation 5 — Event active
        if (!event.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is not active");
        }

        // Validation 6 — Registration deadline
        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration deadline has passed");
        }

        // Validation 3 — Staff exists
        SchoolStaff staff = schoolStaffRepository.findById(request.getCreatedByStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("School staff not found: " + request.getCreatedByStaffId()));

        // Validation 4 — Staff belongs to school
        if (staff.getSchool() == null || !staff.getSchool().getId().equals(school.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff does not belong to the specified school");
        }

        // Validation 7 — Duplicate registration (school_id + event_id)
        if (registrationRepository.existsBySchoolIdAndEventId(school.getId(), event.getId())) {
            throw new RegistrationConflictException("Registration already exists for this school and event");
        }

        // Validation 8 — Event capacity
        long currentRegistrations = registrationRepository.countByEventId(event.getId());
        if (currentRegistrations >= event.getMaxRegistrations()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event registration capacity is full");
        }

        // Map request DTO to Registration entity using ModelMapper & configure associations
        Registration registration = modelMapper.map(request, Registration.class);
        registration.setId(null);
        registration.setSchool(school);
        registration.setEvent(event);
        registration.setCreatedByStaff(staff);
        registration.setStatus(RegistrationStatus.DRAFT);
        if (request.getRemarks() != null) {
            registration.setRemarks(request.getRemarks().trim());
        }
        registration.setCreatedBy(resolvedUser);
        registration.setUpdatedBy(resolvedUser);

        Registration savedRegistration = registrationRepository.save(registration);
        RegistrationResponseDTO responseDto = toDto(savedRegistration);

        audit(resolvedUser, savedRegistration.getId(), AuditAction.CREATE, null, serialize(responseDto));
        return responseDto;
    }

    @Override
    public RegistrationResponseDTO updateRegistration(Long id, RegistrationUpdateRequestDTO request, User currentUser) {
        Registration registration = findRegistration(id);
        final User resolvedUser = resolveCurrentUser(currentUser);

        RegistrationResponseDTO oldDto = toDto(registration);
        String oldValueJson = serialize(oldDto);

        if (request != null && request.getRemarks() != null) {
            registration.setRemarks(request.getRemarks().trim());
        }
        registration.setUpdatedBy(resolvedUser);

        Registration updatedRegistration = registrationRepository.save(registration);
        RegistrationResponseDTO responseDto = toDto(updatedRegistration);

        audit(resolvedUser, updatedRegistration.getId(), AuditAction.UPDATE, oldValueJson, serialize(responseDto));
        return responseDto;
    }

    @Override
    public void deleteRegistration(Long id, User currentUser) {
        Registration registration = findRegistration(id);
        final User resolvedUser = resolveCurrentUser(currentUser);

        long participantCount = participantRepository.countByRegistrationId(id);
        if (participantCount > 0) {
            throw new RegistrationHasParticipantsException("Cannot delete registration because participants exist for this registration");
        }

        RegistrationResponseDTO oldDto = toDto(registration);
        String oldValueJson = serialize(oldDto);

        registrationRepository.delete(registration);

        audit(resolvedUser, id, AuditAction.DELETE, oldValueJson, null);
    }

    @Override
    public RegistrationResponseDTO updateRegistrationStatus(Long id, RegistrationStatusUpdateRequestDTO request, User currentUser) {
        Registration registration = findRegistration(id);
        final User resolvedUser = resolveCurrentUser(currentUser);

        statusValidator.validateTransition(registration.getStatus(), request.getStatus());

        RegistrationResponseDTO oldDto = toDto(registration);
        String oldValueJson = serialize(oldDto);

        registration.setStatus(request.getStatus());
        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            registration.setRemarks(request.getRemarks().trim());
        }
        registration.setUpdatedBy(resolvedUser);

        Registration updatedRegistration = registrationRepository.save(registration);
        RegistrationResponseDTO responseDto = toDto(updatedRegistration);

        audit(resolvedUser, updatedRegistration.getId(), AuditAction.UPDATE, oldValueJson, serialize(responseDto));
        return responseDto;
    }

    @Override
    public RegistrationResponseDTO submitRegistration(Long id, RegistrationRemarksRequestDTO request, User currentUser) {
        Registration registration = findRegistration(id);
        final User resolvedUser = resolveCurrentUser(currentUser);

        statusValidator.validateTransition(registration.getStatus(), RegistrationStatus.PENDING);

        RegistrationResponseDTO oldDto = toDto(registration);
        String oldValueJson = serialize(oldDto);

        registration.setStatus(RegistrationStatus.PENDING);
        if (request != null && request.getRemarks() != null && !request.getRemarks().isBlank()) {
            registration.setRemarks(request.getRemarks().trim());
        }
        registration.setUpdatedBy(resolvedUser);

        Registration updatedRegistration = registrationRepository.save(registration);
        RegistrationResponseDTO responseDto = toDto(updatedRegistration);

        audit(resolvedUser, updatedRegistration.getId(), AuditAction.UPDATE, oldValueJson, serialize(responseDto));
        return responseDto;
    }

    @Override
    public RegistrationResponseDTO approveRegistration(Long id, User currentUser) {
        Registration registration = findRegistration(id);
        final User resolvedUser = resolveCurrentUser(currentUser);

        if (resolvedUser != null && resolvedUser.getRole() != null) {
            String roleCode = resolvedUser.getRole().getRoleCode();
            if ("SCHOOL_STAFF".equals(roleCode) || "LOGIN_TEACHER".equals(roleCode) || "ACCOMPANYING_TEACHER".equals(roleCode)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "School staff members are not authorized to approve registrations. Registrations must be submitted for approval.");
            }
        }

        statusValidator.validateTransition(registration.getStatus(), RegistrationStatus.APPROVED);

        RegistrationResponseDTO oldDto = toDto(registration);
        String oldValueJson = serialize(oldDto);

        registration.setStatus(RegistrationStatus.APPROVED);
        registration.setUpdatedBy(resolvedUser);

        Registration updatedRegistration = registrationRepository.save(registration);
        RegistrationResponseDTO responseDto = toDto(updatedRegistration);

        audit(resolvedUser, updatedRegistration.getId(), AuditAction.UPDATE, oldValueJson, serialize(responseDto));
        return responseDto;
    }

    @Override
    public RegistrationResponseDTO rejectRegistration(Long id, RegistrationRemarksRequestDTO request, User currentUser) {
        Registration registration = findRegistration(id);
        final User resolvedUser = resolveCurrentUser(currentUser);

        if (resolvedUser != null && resolvedUser.getRole() != null) {
            String roleCode = resolvedUser.getRole().getRoleCode();
            if ("SCHOOL_STAFF".equals(roleCode) || "LOGIN_TEACHER".equals(roleCode) || "ACCOMPANYING_TEACHER".equals(roleCode)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "School staff members are not authorized to reject registrations.");
            }
        }

        statusValidator.validateTransition(registration.getStatus(), RegistrationStatus.REJECTED);

        RegistrationResponseDTO oldDto = toDto(registration);
        String oldValueJson = serialize(oldDto);

        registration.setStatus(RegistrationStatus.REJECTED);
        if (request != null && request.getRemarks() != null && !request.getRemarks().isBlank()) {
            registration.setRemarks(request.getRemarks().trim());
        }
        registration.setUpdatedBy(resolvedUser);

        Registration updatedRegistration = registrationRepository.save(registration);
        RegistrationResponseDTO responseDto = toDto(updatedRegistration);

        audit(resolvedUser, updatedRegistration.getId(), AuditAction.UPDATE, oldValueJson, serialize(responseDto));
        return responseDto;
    }

    @Override
    public RegistrationResponseDTO cancelRegistration(Long id, RegistrationRemarksRequestDTO request, User currentUser) {
        Registration registration = findRegistration(id);
        final User resolvedUser = resolveCurrentUser(currentUser);

        statusValidator.validateTransition(registration.getStatus(), RegistrationStatus.CANCELLED);

        RegistrationResponseDTO oldDto = toDto(registration);
        String oldValueJson = serialize(oldDto);

        registration.setStatus(RegistrationStatus.CANCELLED);
        if (request != null && request.getRemarks() != null && !request.getRemarks().isBlank()) {
            registration.setRemarks(request.getRemarks().trim());
        }
        registration.setUpdatedBy(resolvedUser);

        Registration updatedRegistration = registrationRepository.save(registration);
        RegistrationResponseDTO responseDto = toDto(updatedRegistration);

        audit(resolvedUser, updatedRegistration.getId(), AuditAction.UPDATE, oldValueJson, serialize(responseDto));
        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationStatisticsDTO getRegistrationStatistics(Long eventId, Long schoolId) {
        long total;
        long draft;
        long pending;
        long approved;
        long rejected;
        long cancelled;
        long completed;

        if (eventId != null && schoolId != null) {
            total = registrationRepository.countBySchoolIdAndEventId(schoolId, eventId);
            draft = registrationRepository.countBySchoolIdAndEventIdAndStatus(schoolId, eventId, RegistrationStatus.DRAFT);
            pending = registrationRepository.countBySchoolIdAndEventIdAndStatus(schoolId, eventId, RegistrationStatus.PENDING)
                    + registrationRepository.countBySchoolIdAndEventIdAndStatus(schoolId, eventId, RegistrationStatus.SUBMITTED);
            approved = registrationRepository.countBySchoolIdAndEventIdAndStatus(schoolId, eventId, RegistrationStatus.APPROVED);
            rejected = registrationRepository.countBySchoolIdAndEventIdAndStatus(schoolId, eventId, RegistrationStatus.REJECTED);
            cancelled = registrationRepository.countBySchoolIdAndEventIdAndStatus(schoolId, eventId, RegistrationStatus.CANCELLED);
            completed = registrationRepository.countBySchoolIdAndEventIdAndStatus(schoolId, eventId, RegistrationStatus.COMPLETED);
        } else if (eventId != null) {
            total = registrationRepository.countByEventId(eventId);
            draft = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.DRAFT);
            pending = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.PENDING)
                    + registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.SUBMITTED);
            approved = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.APPROVED);
            rejected = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.REJECTED);
            cancelled = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CANCELLED);
            completed = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.COMPLETED);
        } else if (schoolId != null) {
            total = registrationRepository.countBySchoolId(schoolId);
            draft = registrationRepository.countBySchoolIdAndStatus(schoolId, RegistrationStatus.DRAFT);
            pending = registrationRepository.countBySchoolIdAndStatus(schoolId, RegistrationStatus.PENDING)
                    + registrationRepository.countBySchoolIdAndStatus(schoolId, RegistrationStatus.SUBMITTED);
            approved = registrationRepository.countBySchoolIdAndStatus(schoolId, RegistrationStatus.APPROVED);
            rejected = registrationRepository.countBySchoolIdAndStatus(schoolId, RegistrationStatus.REJECTED);
            cancelled = registrationRepository.countBySchoolIdAndStatus(schoolId, RegistrationStatus.CANCELLED);
            completed = registrationRepository.countBySchoolIdAndStatus(schoolId, RegistrationStatus.COMPLETED);
        } else {
            total = registrationRepository.count();
            draft = registrationRepository.countByStatus(RegistrationStatus.DRAFT);
            pending = registrationRepository.countByStatus(RegistrationStatus.PENDING)
                    + registrationRepository.countByStatus(RegistrationStatus.SUBMITTED);
            approved = registrationRepository.countByStatus(RegistrationStatus.APPROVED);
            rejected = registrationRepository.countByStatus(RegistrationStatus.REJECTED);
            cancelled = registrationRepository.countByStatus(RegistrationStatus.CANCELLED);
            completed = registrationRepository.countByStatus(RegistrationStatus.COMPLETED);
        }

        return RegistrationStatisticsDTO.builder()
                .total(total)
                .draft(draft)
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .cancelled(cancelled)
                .completed(completed)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistrationResponseDTO> getRegistrationsForEvent(
            Long eventId,
            String search,
            List<RegistrationStatus> statuses,
            int page,
            int size,
            String sort
    ) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found: " + eventId);
        }
        return getRegistrations(search, statuses, null, eventId, null, null, null, null, null, page, size, sort);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistrationResponseDTO> getRegistrationsForSchool(
            Long schoolId,
            String search,
            List<RegistrationStatus> statuses,
            Long eventId,
            int page,
            int size,
            String sort,
            User currentUser
    ) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found: " + schoolId);
        }
        checkSchoolAccess(schoolId, currentUser);
        return getRegistrations(search, statuses, schoolId, eventId, null, null, null, null, null, page, size, sort);
    }

    private void checkSchoolAccess(Long schoolId, User currentUser) {
        User resolvedUser = resolveCurrentUser(currentUser);
        if (resolvedUser == null || resolvedUser.getRole() == null) {
            return;
        }

        String roleCode = resolvedUser.getRole().getRoleCode();
        if ("SUPER_ADMIN".equals(roleCode) || "ADMIN".equals(roleCode) || "EVENT_MANAGER".equals(roleCode)) {
            return;
        }

        List<Long> staffSchoolIds = schoolStaffRepository.findSchoolIdsByUserId(resolvedUser.getId());
        List<Long> createdSchoolIds = schoolRepository.findSchoolIdsByCreatedById(resolvedUser.getId());
        boolean hasAccess = staffSchoolIds.contains(schoolId) || createdSchoolIds.contains(schoolId);

        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view registrations for this school");
        }
    }

    private Registration findRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new RegistrationNotFoundException("Registration not found: " + id));
    }

    private Pageable createPageable(int page, int size, String sortParam) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sort = buildSort(sortParam);
        return PageRequest.of(safePage, safeSize, sort);
    }

    private Sort buildSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")) {
            direction = Sort.Direction.DESC;
        }

        return switch (field) {
            case "id" -> Sort.by(direction, "id");
            case "createdAt" -> Sort.by(direction, "createdAt");
            case "updatedAt" -> Sort.by(direction, "updatedAt");
            case "status" -> Sort.by(direction, "status");
            case "eventDate" -> Sort.by(direction, "event.eventDate");
            case "schoolName" -> Sort.by(direction, "school.schoolName");
            case "eventName" -> Sort.by(direction, "event.eventName");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private Specification<Registration> buildSpecification(
            String search,
            List<RegistrationStatus> statuses,
            Long schoolId,
            Long eventId,
            Long createdByStaffId,
            LocalDate createdFrom,
            LocalDate createdTo,
            LocalDate eventDateFrom,
            LocalDate eventDateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate schoolNameMatch = cb.like(cb.lower(root.get("school").get("schoolName")), pattern);
                Predicate schoolCodeMatch = cb.like(cb.lower(root.get("school").get("schoolCode")), pattern);
                Predicate eventNameMatch = cb.like(cb.lower(root.get("event").get("eventName")), pattern);
                Predicate staffNameMatch = cb.like(cb.lower(root.get("createdByStaff").get("fullName")), pattern);

                Predicate searchPredicate = cb.or(schoolNameMatch, schoolCodeMatch, eventNameMatch, staffNameMatch);
                try {
                    Long searchId = Long.parseLong(search.trim());
                    Predicate idMatch = cb.equal(root.get("id"), searchId);
                    searchPredicate = cb.or(searchPredicate, idMatch);
                } catch (NumberFormatException ignored) {
                }
                predicates.add(searchPredicate);
            }

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            if (schoolId != null) {
                predicates.add(cb.equal(root.get("school").get("id"), schoolId));
            }

            if (eventId != null) {
                predicates.add(cb.equal(root.get("event").get("id"), eventId));
            }

            if (createdByStaffId != null) {
                predicates.add(cb.equal(root.get("createdByStaff").get("id"), createdByStaffId));
            }

            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom.atStartOfDay()));
            }

            if (createdTo != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), createdTo.plusDays(1).atStartOfDay()));
            }

            if (eventDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("event").get("eventDate"), eventDateFrom));
            }

            if (eventDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("event").get("eventDate"), eventDateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private User resolveCurrentUser(User currentUser) {
        if (currentUser != null && currentUser.getId() != null) {
            return currentUser;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        if (authentication != null && authentication.getName() != null) {
            return userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        return null;
    }

    private RegistrationResponseDTO toDto(Registration registration) {
        if (registration == null) {
            return null;
        }

        SchoolSummaryDTO schoolSummary = null;
        if (registration.getSchool() != null) {
            schoolSummary = SchoolSummaryDTO.builder()
                    .id(registration.getSchool().getId())
                    .schoolCode(registration.getSchool().getSchoolCode())
                    .schoolName(registration.getSchool().getSchoolName())
                    .build();
        }

        EventSummaryDTO eventSummary = null;
        if (registration.getEvent() != null) {
            eventSummary = EventSummaryDTO.builder()
                    .id(registration.getEvent().getId())
                    .eventName(registration.getEvent().getEventName())
                    .build();
        }

        StaffSummaryDTO staffSummary = null;
        if (registration.getCreatedByStaff() != null) {
            staffSummary = StaffSummaryDTO.builder()
                    .id(registration.getCreatedByStaff().getId())
                    .fullName(registration.getCreatedByStaff().getFullName())
                    .build();
        }

        long participantCount = 0L;
        if (registration.getParticipants() != null && !registration.getParticipants().isEmpty()) {
            participantCount = registration.getParticipants().size();
        } else if (registration.getId() != null) {
            participantCount = participantRepository.countByRegistrationId(registration.getId());
        }

        return RegistrationResponseDTO.builder()
                .id(registration.getId())
                .school(schoolSummary)
                .event(eventSummary)
                .createdByStaff(staffSummary)
                .status(registration.getStatus())
                .remarks(registration.getRemarks())
                .participantCount(participantCount)
                .createdAt(registration.getCreatedAt())
                .updatedAt(registration.getUpdatedAt())
                .build();
    }

    private void audit(User user, Long entityId, AuditAction action, String oldValueJson, String newValueJson) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .entityName("Registration")
                    .entityId(entityId)
                    .action(action)
                    .status(AuditStatus.SUCCESS)
                    .oldValue(oldValueJson)
                    .newValue(newValueJson)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Do not break transaction if audit serialization fails
        }
    }

    private String serialize(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            return null;
        }
    }
}
