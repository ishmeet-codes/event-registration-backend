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
import com.registration.management.event.entities.ParticipationCategory;
import com.registration.management.event.repository.EventRepository;
import com.registration.management.participant.dto.ParticipantCreateDTO;
import com.registration.management.participant.dto.ParticipantResponseDTO;
import com.registration.management.participant.entity.Participant;
import com.registration.management.participant.entity.ParticipantEvent;
import com.registration.management.participant.repository.ParticipantEventRepository;
import com.registration.management.participant.repository.ParticipantRepository;
import com.registration.management.registration.dto.*;
import com.registration.management.registration.entity.Registration;
import com.registration.management.registration.entity.RegistrationEvent;
import com.registration.management.registration.exception.RegistrationHasParticipantsException;
import com.registration.management.registration.exception.RegistrationNotFoundException;
import com.registration.management.registration.repository.RegistrationEventRepository;
import com.registration.management.registration.repository.RegistrationRepository;
import com.registration.management.registration.service.RegistrationService;
import com.registration.management.registration.service.RegistrationStatusValidator;
import com.registration.management.school.entity.School;
import com.registration.management.school.entity.SchoolStaff;
import com.registration.management.school.repository.schoolRepository;
import com.registration.management.school.repository.schoolStaffRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final RegistrationEventRepository registrationEventRepository;
    private final ParticipantEventRepository participantEventRepository;
    private final schoolRepository schoolRepository;
    private final EventRepository eventRepository;
    private final schoolStaffRepository schoolStaffRepository;
    private final ParticipantRepository participantRepository;
    private final RegistrationStatusValidator statusValidator;
    private final UserRepository userRepository;
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
    @Transactional
    public RegistrationResponseDTO createRegistration(RegistrationCreateRequestDTO request, User currentUser) {
        final User resolvedUser = resolveCurrentUser(currentUser);

        // Validation 1 — School exists & active
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found: " + request.getSchoolId()));
        if (!school.isActive()) {
            throw new SchoolNotActiveException("School is not active");
        }

        // Validation 2 — Staff authorization check
        SchoolStaff staff = null;
        if (resolvedUser != null) {
            staff = schoolStaffRepository.findFirstByUserIdAndSchoolIdAndActiveTrue(resolvedUser.getId(), school.getId()).orElse(null);
            if (staff == null) {
                List<Long> userSchoolIds = schoolStaffRepository.findSchoolIdsByUserId(resolvedUser.getId());
                if (!userSchoolIds.isEmpty() && !userSchoolIds.contains(school.getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff member is not authorized for school ID " + school.getId());
                }
                staff = schoolStaffRepository.findFirstByUserIdAndActiveTrue(resolvedUser.getId()).orElse(null);
            }
        }
        if (staff == null && request.getCreatedByStaffId() != null) {
            staff = schoolStaffRepository.findById(request.getCreatedByStaffId()).orElse(null);
        }
        if (staff == null) {
            staff = schoolStaffRepository.findAll().stream()
                    .filter(s -> s.getSchool().getId().equals(school.getId()) && s.isActive())
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active school staff found for school ID " + school.getId()));
        }

        // Collect target event IDs
        List<Long> targetEventIds = new ArrayList<>();
        if (request.getEventIds() != null && !request.getEventIds().isEmpty()) {
            targetEventIds.addAll(request.getEventIds());
        }
        if (request.getEventId() != null && !targetEventIds.contains(request.getEventId())) {
            targetEventIds.add(request.getEventId());
        }
        if (targetEventIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one event must be selected for the registration");
        }

        // Validate events & build event map
        Map<Long, Event> eventMap = new HashMap<>();
        for (Long eId : targetEventIds) {
            Event event = eventRepository.findById(eId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eId));

            if (!event.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is not active: " + event.getEventName());
            }

            if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration deadline has passed for event: " + event.getEventName());
            }

            long currentRegs = registrationRepository.countByEventId(event.getId());
            if (currentRegs >= event.getMaxRegistrations()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration capacity full for event: " + event.getEventName());
            }

            eventMap.put(eId, event);
        }

        // Pre-validate per-event participant assignment counts
        Map<Long, Integer> eventAssignedCounts = new HashMap<>();
        for (Long eId : targetEventIds) {
            eventAssignedCounts.put(eId, 0);
        }

        if (request.getParticipants() != null) {
            for (ParticipantCreateDTO pDto : request.getParticipants()) {
                List<Long> pEvents = pDto.getEventIds();
                if (pEvents == null || pEvents.isEmpty()) {
                    pEvents = targetEventIds; // Default assign to all if unspecified
                }
                for (Long peId : pEvents) {
                    if (!eventMap.containsKey(peId)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Participant is assigned to an unselected event ID: " + peId);
                    }
                    eventAssignedCounts.put(peId, eventAssignedCounts.get(peId) + 1);
                }
            }
        }

        // Check participation category limits per event
        for (Long eId : targetEventIds) {
            Event ev = eventMap.get(eId);
            if (ev.getParticipationCategory() != null) {
                ParticipationCategory cat = ev.getParticipationCategory();
                int count = eventAssignedCounts.getOrDefault(eId, 0);
                if (count < cat.getMinParticipants()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Participant count (" + count + ") is below minimum required (" + cat.getMinParticipants() + ") for event: " + ev.getEventName()
                    );
                }
                if (count > cat.getMaxParticipants()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Participant count (" + count + ") exceeds maximum allowed (" + cat.getMaxParticipants() + ") for event: " + ev.getEventName()
                    );
                }
            }
        }

        // 1. Create SINGLE Registration entity
        Registration registration = new Registration();
        registration.setSchool(school);
        registration.setCreatedByStaff(staff);
        registration.setStatus(RegistrationStatus.PENDING);
        if (request.getRemarks() != null) {
            registration.setRemarks(request.getRemarks().trim());
        }
        registration.setCreatedBy(resolvedUser);
        registration.setUpdatedBy(resolvedUser);

        Registration savedRegistration = registrationRepository.save(registration);

        // 2. Save RegistrationEvent join records
        for (Long eId : targetEventIds) {
            Event ev = eventMap.get(eId);
            RegistrationEvent re = RegistrationEvent.builder()
                    .registration(savedRegistration)
                    .event(ev)
                    .build();
            registrationEventRepository.save(re);
            savedRegistration.getRegistrationEvents().add(re);
        }

        // 3. Save Participant and ParticipantEvent join records
        if (request.getParticipants() != null && !request.getParticipants().isEmpty()) {
            for (ParticipantCreateDTO pDto : request.getParticipants()) {
                Participant participant = Participant.builder()
                        .registration(savedRegistration)
                        .fullName(pDto.getFullName().trim())
                        .gender(pDto.getGender())
                        .className(pDto.getClassName() != null ? pDto.getClassName().trim() : null)
                        .dob(pDto.getDob())
                        .guardianPhone(pDto.getGuardianPhone().trim())
                        .createdBy(resolvedUser)
                        .updatedBy(resolvedUser)
                        .build();

                Participant savedParticipant = participantRepository.save(participant);

                List<Long> pEvents = pDto.getEventIds();
                if (pEvents == null || pEvents.isEmpty()) {
                    pEvents = targetEventIds;
                }

                for (Long peId : pEvents) {
                    Event ev = eventMap.get(peId);
                    ParticipantEvent pe = ParticipantEvent.builder()
                            .participant(savedParticipant)
                            .event(ev)
                            .build();
                    participantEventRepository.save(pe);
                    savedParticipant.getParticipantEvents().add(pe);
                }

                savedRegistration.addParticipant(savedParticipant);
            }
        }

        RegistrationResponseDTO responseDto = toDto(savedRegistration);
        audit(resolvedUser, savedRegistration.getId(), AuditAction.CREATE, null, serialize(responseDto));
        return responseDto;
    }

    @Override
    @Transactional
    public BulkRegistrationResponseDTO createBulkRegistrations(BulkRegistrationRequestDTO request, User currentUser) {
        final User resolvedUser = resolveCurrentUser(currentUser);

        // Map BulkRegistrationRequestDTO to RegistrationCreateRequestDTO (single registration application)
        List<Long> allEventIds = request.getRegistrations().stream()
                .map(BulkRegistrationItemRequestDTO::getEventId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, List<Long>> participantEventsMap = new HashMap<>();
        for (BulkRegistrationItemRequestDTO item : request.getRegistrations()) {
            for (String cid : item.getParticipantClientIds()) {
                participantEventsMap.computeIfAbsent(cid, k -> new ArrayList<>()).add(item.getEventId());
            }
        }

        List<ParticipantCreateDTO> pList = new ArrayList<>();
        for (BulkParticipantDTO bp : request.getParticipants()) {
            ParticipantCreateDTO pDto = ParticipantCreateDTO.builder()
                    .fullName(bp.getFullName())
                    .gender(bp.getGender())
                    .className(bp.getClassName())
                    .dob(bp.getDob())
                    .guardianPhone(bp.getGuardianPhone())
                    .eventIds(participantEventsMap.getOrDefault(bp.getClientId(), allEventIds))
                    .build();
            pList.add(pDto);
        }

        RegistrationCreateRequestDTO singleReq = RegistrationCreateRequestDTO.builder()
                .schoolId(request.getSchoolId())
                .eventIds(allEventIds)
                .remarks(request.getRemarks())
                .participants(pList)
                .build();

        RegistrationResponseDTO createdReg = createRegistration(singleReq, currentUser);

        List<BulkRegistrationItemResponseDTO> itemResponses = createdReg.getEvents().stream()
                .map(e -> BulkRegistrationItemResponseDTO.builder()
                        .registrationId(createdReg.getId())
                        .eventId(e.getId())
                        .eventName(e.getEventName())
                        .participantCount(createdReg.getParticipantCount() != null ? createdReg.getParticipantCount().intValue() : 0)
                        .build())
                .collect(Collectors.toList());

        return BulkRegistrationResponseDTO.builder()
                .submissionId(UUID.randomUUID().toString())
                .schoolId(createdReg.getSchool() != null ? createdReg.getSchool().getId() : request.getSchoolId())
                .registrationsCreated(1)
                .participantsSubmitted(createdReg.getParticipants() != null ? createdReg.getParticipants().size() : 0)
                .registrations(itemResponses)
                .build();
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

        long count = registration.getParticipants() != null && !registration.getParticipants().isEmpty()
                ? registration.getParticipants().size()
                : participantRepository.countByRegistrationId(id);

        if (count > 0) {
            throw new RegistrationHasParticipantsException("Cannot delete registration with ID " + id + " because it contains " + count + " participant(s)");
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

        RegistrationResponseDTO oldDto = toDto(registration);
        String oldValueJson = serialize(oldDto);

        statusValidator.validateTransition(registration.getStatus(), request.getStatus());
        registration.setStatus(request.getStatus());
        if (request.getRemarks() != null) {
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

        RegistrationResponseDTO oldDto = toDto(registration);
        String oldValueJson = serialize(oldDto);

        statusValidator.validateTransition(registration.getStatus(), RegistrationStatus.SUBMITTED);

        long participantCount = registration.getParticipants() != null ? registration.getParticipants().size() : 0L;
        if (participantCount == 0) {
            throw new RegistrationHasParticipantsException("Cannot submit registration with zero participants");
        }

        registration.setStatus(RegistrationStatus.SUBMITTED);
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
    public RegistrationResponseDTO approveRegistration(Long id, User currentUser) {
        RegistrationStatusUpdateRequestDTO request = new RegistrationStatusUpdateRequestDTO();
        request.setStatus(RegistrationStatus.APPROVED);
        return updateRegistrationStatus(id, request, currentUser);
    }

    @Override
    public RegistrationResponseDTO rejectRegistration(Long id, RegistrationRemarksRequestDTO request, User currentUser) {
        RegistrationStatusUpdateRequestDTO statusRequest = new RegistrationStatusUpdateRequestDTO();
        statusRequest.setStatus(RegistrationStatus.REJECTED);
        if (request != null) {
            statusRequest.setRemarks(request.getRemarks());
        }
        return updateRegistrationStatus(id, statusRequest, currentUser);
    }

    @Override
    public RegistrationResponseDTO cancelRegistration(Long id, RegistrationRemarksRequestDTO request, User currentUser) {
        RegistrationStatusUpdateRequestDTO statusRequest = new RegistrationStatusUpdateRequestDTO();
        statusRequest.setStatus(RegistrationStatus.CANCELLED);
        if (request != null) {
            statusRequest.setRemarks(request.getRemarks());
        }
        return updateRegistrationStatus(id, statusRequest, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationStatisticsDTO getRegistrationStatistics(Long eventId, Long schoolId) {
        if (eventId != null || schoolId != null) {
            long total = 0L;
            long draft = 0L;
            long pending = 0L;
            long submitted = 0L;
            long approved = 0L;
            long rejected = 0L;
            long cancelled = 0L;
            long completed = 0L;

            for (RegistrationStatus st : RegistrationStatus.values()) {
                long c = 0L;
                if (eventId != null && schoolId != null) {
                    c = registrationRepository.countBySchoolIdAndEventIdAndStatus(schoolId, eventId, st);
                } else if (eventId != null) {
                    c = registrationRepository.countByEventIdAndStatus(eventId, st);
                } else {
                    c = registrationRepository.countBySchoolIdAndStatus(schoolId, st);
                }

                switch (st) {
                    case DRAFT -> draft = c;
                    case PENDING -> pending = c;
                    case SUBMITTED -> submitted = c;
                    case APPROVED -> approved = c;
                    case REJECTED -> rejected = c;
                    case CANCELLED -> cancelled = c;
                    case COMPLETED -> completed = c;
                }
                total += c;
            }

            return RegistrationStatisticsDTO.builder()
                    .total(total)
                    .draft(draft)
                    .pending(pending + submitted)
                    .approved(approved)
                    .rejected(rejected)
                    .cancelled(cancelled)
                    .completed(completed)
                    .build();
        }

        long total = registrationRepository.count();
        long draft = registrationRepository.countByStatus(RegistrationStatus.DRAFT);
        long pending = registrationRepository.countByStatus(RegistrationStatus.PENDING);
        long submitted = registrationRepository.countByStatus(RegistrationStatus.SUBMITTED);
        long approved = registrationRepository.countByStatus(RegistrationStatus.APPROVED);
        long rejected = registrationRepository.countByStatus(RegistrationStatus.REJECTED);
        long cancelled = registrationRepository.countByStatus(RegistrationStatus.CANCELLED);
        long completed = registrationRepository.countByStatus(RegistrationStatus.COMPLETED);

        return RegistrationStatisticsDTO.builder()
                .total(total)
                .draft(draft)
                .pending(pending + submitted)
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
        Pageable pageable = createPageable(page, size, sort);
        Specification<Registration> spec = buildSpecification(
                search, statuses, null, eventId, null, null, null, null, null
        );
        return registrationRepository.findAll(spec, pageable).map(this::toDto);
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
        Pageable pageable = createPageable(page, size, sort);
        Specification<Registration> spec = buildSpecification(
                search, statuses, schoolId, eventId, null, null, null, null, null
        );
        return registrationRepository.findAll(spec, pageable).map(this::toDto);
    }

    private Registration findRegistration(Long id) {
        return registrationRepository.findByIdWithDetails(id)
                .or(() -> registrationRepository.findById(id))
                .orElseThrow(() -> new RegistrationNotFoundException("Registration not found: " + id));
    }

    private Pageable createPageable(int page, int size, String sort) {
        int pageNumber = Math.max(0, page);
        int pageSize = size <= 0 ? 20 : size;

        String[] sortParams = sort != null ? sort.split(",") : new String[]{"createdAt", "desc"};
        String sortProperty = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortProperty));
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

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate schoolName = cb.like(cb.lower(root.get("school").get("schoolName")), searchPattern);
                Predicate schoolCode = cb.like(cb.lower(root.get("school").get("schoolCode")), searchPattern);
                predicates.add(cb.or(schoolName, schoolCode));
            }

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            if (schoolId != null) {
                predicates.add(cb.equal(root.get("school").get("id"), schoolId));
            }

            if (eventId != null) {
                var join = root.join("registrationEvents");
                predicates.add(cb.equal(join.get("event").get("id"), eventId));
            }

            if (createdByStaffId != null) {
                predicates.add(cb.equal(root.get("createdByStaff").get("id"), createdByStaffId));
            }

            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom.atStartOfDay()));
            }

            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo.atTime(23, 59, 59)));
            }

            if (query != null && query.getResultType() != Long.class) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private User resolveCurrentUser(User currentUser) {
        if (currentUser != null) {
            return currentUser;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (authentication.getPrincipal() instanceof User user) {
                return user;
            }
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

        StaffSummaryDTO staffSummary = null;
        if (registration.getCreatedByStaff() != null) {
            staffSummary = StaffSummaryDTO.builder()
                    .id(registration.getCreatedByStaff().getId())
                    .fullName(registration.getCreatedByStaff().getFullName())
                    .build();
        }

        List<EventSummaryDTO> eventSummaries = new ArrayList<>();
        if (registration.getRegistrationEvents() != null && !registration.getRegistrationEvents().isEmpty()) {
            for (RegistrationEvent re : registration.getRegistrationEvents()) {
                if (re.getEvent() != null) {
                    eventSummaries.add(EventSummaryDTO.builder()
                            .id(re.getEvent().getId())
                            .eventName(re.getEvent().getEventName())
                            .build());
                }
            }
        }
        EventSummaryDTO firstEvent = !eventSummaries.isEmpty() ? eventSummaries.get(0) : null;

        long participantCount = 0L;
        List<ParticipantResponseDTO> participantDtos = null;
        if (registration.getParticipants() != null && !registration.getParticipants().isEmpty()) {
            participantCount = registration.getParticipants().size();
            participantDtos = new ArrayList<>();
            for (Participant p : registration.getParticipants()) {
                List<EventSummaryDTO> pEventSummaries = new ArrayList<>();
                if (p.getParticipantEvents() != null) {
                    for (ParticipantEvent pe : p.getParticipantEvents()) {
                        if (pe.getEvent() != null) {
                            pEventSummaries.add(EventSummaryDTO.builder()
                                    .id(pe.getEvent().getId())
                                    .eventName(pe.getEvent().getEventName())
                                    .build());
                        }
                    }
                }
                participantDtos.add(ParticipantResponseDTO.builder()
                        .id(p.getId())
                        .registrationId(registration.getId())
                        .fullName(p.getFullName())
                        .gender(p.getGender())
                        .className(p.getClassName())
                        .dob(p.getDob())
                        .guardianPhone(p.getGuardianPhone())
                        .events(pEventSummaries)
                        .createdAt(p.getCreatedAt())
                        .updatedAt(p.getUpdatedAt())
                        .build());
            }
        } else if (registration.getId() != null) {
            participantCount = participantRepository.countByRegistrationId(registration.getId());
        }

        return RegistrationResponseDTO.builder()
                .id(registration.getId())
                .school(schoolSummary)
                .event(firstEvent)
                .events(eventSummaries)
                .eventCount((long) eventSummaries.size())
                .createdByStaff(staffSummary)
                .status(registration.getStatus())
                .remarks(registration.getRemarks())
                .participantCount(participantCount)
                .participants(participantDtos)
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
