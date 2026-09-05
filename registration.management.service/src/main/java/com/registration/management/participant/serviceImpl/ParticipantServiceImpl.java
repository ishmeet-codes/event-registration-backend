package com.registration.management.participant.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.registration.management.audit.entities.AuditLog;
import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.common.exception.ResourceNotFoundException;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
import com.registration.management.enums.Gender;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.event.entities.Event;
import com.registration.management.event.entities.ParticipationCategory;
import com.registration.management.participant.dto.*;
import com.registration.management.participant.entity.Participant;
import com.registration.management.participant.entity.ParticipantEvent;
import com.registration.management.participant.exception.ParticipantLimitExceededException;
import com.registration.management.participant.exception.ParticipantNotFoundException;
import com.registration.management.participant.repository.ParticipantEventRepository;
import com.registration.management.participant.repository.ParticipantRepository;
import com.registration.management.participant.service.ParticipantService;
import com.registration.management.registration.dto.EventSummaryDTO;
import com.registration.management.registration.dto.SchoolSummaryDTO;
import com.registration.management.registration.entity.Registration;
import com.registration.management.registration.exception.RegistrationNotFoundException;
import com.registration.management.registration.repository.RegistrationRepository;
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
public class ParticipantServiceImpl implements ParticipantService {

    private final ParticipantRepository participantRepository;
    private final ParticipantEventRepository participantEventRepository;
    private final RegistrationRepository registrationRepository;
    private final schoolStaffRepository schoolStaffRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    @Transactional(readOnly = true)
    public ParticipantResponseDTO getParticipantById(Long id, User currentUser) {
        Participant participant = findParticipant(id);
        User resolvedUser = resolveCurrentUser(currentUser);
        checkSchoolAccess(participant.getRegistration().getSchool().getId(), resolvedUser);
        return toDto(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ParticipantResponseDTO> getParticipants(
            String search,
            Long registrationId,
            Long schoolId,
            Long eventId,
            Gender gender,
            String className,
            LocalDate dobFrom,
            LocalDate dobTo,
            int page,
            int size,
            String sort,
            User currentUser
    ) {
        User resolvedUser = resolveCurrentUser(currentUser);
        if (schoolId != null) {
            checkSchoolAccess(schoolId, resolvedUser);
        }

        Pageable pageable = createPageable(page, size, sort);
        Specification<Participant> spec = buildSpecification(
                search, registrationId, schoolId, eventId, gender, className, dobFrom, dobTo
        );
        return participantRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Override
    public ParticipantResponseDTO createParticipant(ParticipantCreateRequestDTO request, User currentUser) {
        User resolvedUser = resolveCurrentUser(currentUser);

        Registration registration = registrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new RegistrationNotFoundException("Registration not found: " + request.getRegistrationId()));

        checkSchoolAccess(registration.getSchool().getId(), resolvedUser);

        if (registration.getStatus() == RegistrationStatus.CANCELLED || registration.getStatus() == RegistrationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot add participants to a " + registration.getStatus() + " registration");
        }

        Participant participant = Participant.builder()
                .registration(registration)
                .fullName(request.getFullName().trim())
                .gender(request.getGender())
                .className(request.getClassName() != null ? request.getClassName().trim() : null)
                .dob(request.getDob())
                .guardianPhone(request.getGuardianPhone().trim())
                .createdBy(resolvedUser)
                .updatedBy(resolvedUser)
                .build();

        Participant savedParticipant = participantRepository.save(participant);
        ParticipantResponseDTO responseDto = toDto(savedParticipant);

        audit(resolvedUser, savedParticipant.getId(), AuditAction.CREATE, null, serialize(responseDto));
        return responseDto;
    }

    @Override
    public ParticipantResponseDTO updateParticipant(Long id, ParticipantUpdateRequestDTO request, User currentUser) {
        Participant participant = findParticipant(id);
        User resolvedUser = resolveCurrentUser(currentUser);
        checkSchoolAccess(participant.getRegistration().getSchool().getId(), resolvedUser);

        ParticipantResponseDTO oldDto = toDto(participant);
        String oldValueJson = serialize(oldDto);

        if (request.getFullName() != null) {
            participant.setFullName(request.getFullName().trim());
        }
        if (request.getGender() != null) {
            participant.setGender(request.getGender());
        }
        if (request.getClassName() != null) {
            participant.setClassName(request.getClassName().trim());
        }
        if (request.getDob() != null) {
            participant.setDob(request.getDob());
        }
        if (request.getGuardianPhone() != null) {
            participant.setGuardianPhone(request.getGuardianPhone().trim());
        }
        participant.setUpdatedBy(resolvedUser);

        Participant updatedParticipant = participantRepository.save(participant);
        ParticipantResponseDTO responseDto = toDto(updatedParticipant);

        audit(resolvedUser, updatedParticipant.getId(), AuditAction.UPDATE, oldValueJson, serialize(responseDto));
        return responseDto;
    }

    @Override
    public void deleteParticipant(Long id, User currentUser) {
        Participant participant = findParticipant(id);
        User resolvedUser = resolveCurrentUser(currentUser);
        checkSchoolAccess(participant.getRegistration().getSchool().getId(), resolvedUser);

        ParticipantResponseDTO oldDto = toDto(participant);
        String oldValueJson = serialize(oldDto);

        participantRepository.delete(participant);
        audit(resolvedUser, id, AuditAction.DELETE, oldValueJson, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ParticipantResponseDTO> getParticipantsByRegistration(
            Long registrationId,
            String search,
            Gender gender,
            String className,
            int page,
            int size,
            String sort,
            User currentUser
    ) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RegistrationNotFoundException("Registration not found: " + registrationId));

        User resolvedUser = resolveCurrentUser(currentUser);
        checkSchoolAccess(registration.getSchool().getId(), resolvedUser);

        Pageable pageable = createPageable(page, size, sort);
        Specification<Participant> spec = buildSpecification(search, registrationId, null, null, gender, className, null, null);
        return participantRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ParticipantResponseDTO> getParticipantsBySchool(
            Long schoolId,
            String search,
            Long eventId,
            Gender gender,
            String className,
            int page,
            int size,
            String sort,
            User currentUser
    ) {
        User resolvedUser = resolveCurrentUser(currentUser);
        checkSchoolAccess(schoolId, resolvedUser);

        Pageable pageable = createPageable(page, size, sort);
        Specification<Participant> spec = buildSpecification(search, null, schoolId, eventId, gender, className, null, null);
        return participantRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ParticipantResponseDTO> getParticipantsByEvent(
            Long eventId,
            String search,
            Long schoolId,
            Gender gender,
            String className,
            int page,
            int size,
            String sort,
            User currentUser
    ) {
        User resolvedUser = resolveCurrentUser(currentUser);
        if (schoolId != null) {
            checkSchoolAccess(schoolId, resolvedUser);
        }

        Pageable pageable = createPageable(page, size, sort);
        Specification<Participant> spec = buildSpecification(search, null, schoolId, eventId, gender, className, null, null);
        return participantRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantCountResponseDTO getParticipantCount(Long registrationId, User currentUser) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RegistrationNotFoundException("Registration not found: " + registrationId));

        User resolvedUser = resolveCurrentUser(currentUser);
        checkSchoolAccess(registration.getSchool().getId(), resolvedUser);

        long count = participantRepository.countByRegistrationId(registrationId);

        return ParticipantCountResponseDTO.builder()
                .registrationId(registrationId)
                .participantCount(count)
                .minimumParticipants(1)
                .maximumParticipants(100)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantEligibilityResponseDTO checkEligibility(Long id, User currentUser) {
        Participant participant = findParticipant(id);
        User resolvedUser = resolveCurrentUser(currentUser);
        checkSchoolAccess(participant.getRegistration().getSchool().getId(), resolvedUser);

        List<String> reasons = new ArrayList<>();
        Registration registration = participant.getRegistration();

        if (registration == null) {
            reasons.add("REGISTRATION_NOT_FOUND");
        } else {
            if (registration.getStatus() == RegistrationStatus.CANCELLED || registration.getStatus() == RegistrationStatus.REJECTED) {
                reasons.add("REGISTRATION_NOT_ACTIVE");
            }
        }

        boolean eligible = reasons.isEmpty();
        return ParticipantEligibilityResponseDTO.builder()
                .participantId(id)
                .eligible(eligible)
                .reasons(reasons)
                .build();
    }

    private Participant findParticipant(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new ParticipantNotFoundException("Participant not found: " + id));
    }

    private void checkSchoolAccess(Long schoolId, User user) {
        if (user == null) {
            return;
        }
        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleCode())) {
            return;
        }
        List<Long> userSchoolIds = schoolStaffRepository.findSchoolIdsByUserId(user.getId());
        if (!userSchoolIds.isEmpty() && !userSchoolIds.contains(schoolId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have access to school ID: " + schoolId);
        }
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

    private Specification<Participant> buildSpecification(
            String search,
            Long registrationId,
            Long schoolId,
            Long eventId,
            Gender gender,
            String className,
            LocalDate dobFrom,
            LocalDate dobTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate fullName = cb.like(cb.lower(root.get("fullName")), searchPattern);
                Predicate guardianPhone = cb.like(cb.lower(root.get("guardianPhone")), searchPattern);
                predicates.add(cb.or(fullName, guardianPhone));
            }

            if (registrationId != null) {
                predicates.add(cb.equal(root.get("registration").get("id"), registrationId));
            }

            if (schoolId != null) {
                predicates.add(cb.equal(root.get("registration").get("school").get("id"), schoolId));
            }

            if (eventId != null) {
                var join = root.join("participantEvents");
                predicates.add(cb.equal(join.get("event").get("id"), eventId));
            }

            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }

            if (className != null && !className.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("className")), "%" + className.trim().toLowerCase() + "%"));
            }

            if (dobFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dob"), dobFrom));
            }

            if (dobTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dob"), dobTo));
            }

            if (query != null && query.getResultType() != Long.class) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ParticipantResponseDTO toDto(Participant participant) {
        if (participant == null) {
            return null;
        }

        SchoolSummaryDTO schoolSummary = null;
        Registration reg = participant.getRegistration();

        if (reg != null && reg.getSchool() != null) {
            schoolSummary = SchoolSummaryDTO.builder()
                    .id(reg.getSchool().getId())
                    .schoolCode(reg.getSchool().getSchoolCode())
                    .schoolName(reg.getSchool().getSchoolName())
                    .build();
        }

        List<EventSummaryDTO> eventSummaries = new ArrayList<>();
        if (participant.getParticipantEvents() != null && !participant.getParticipantEvents().isEmpty()) {
            for (ParticipantEvent pe : participant.getParticipantEvents()) {
                if (pe.getEvent() != null) {
                    eventSummaries.add(EventSummaryDTO.builder()
                            .id(pe.getEvent().getId())
                            .eventName(pe.getEvent().getEventName())
                            .build());
                }
            }
        }
        EventSummaryDTO firstEvent = !eventSummaries.isEmpty() ? eventSummaries.get(0) : null;

        return ParticipantResponseDTO.builder()
                .id(participant.getId())
                .registrationId(participant.getRegistration() != null ? participant.getRegistration().getId() : null)
                .fullName(participant.getFullName())
                .gender(participant.getGender())
                .className(participant.getClassName())
                .dob(participant.getDob())
                .guardianPhone(participant.getGuardianPhone())
                .school(schoolSummary)
                .event(firstEvent)
                .events(eventSummaries)
                .createdAt(participant.getCreatedAt())
                .updatedAt(participant.getUpdatedAt())
                .build();
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

    private void audit(User user, Long entityId, AuditAction action, String oldValueJson, String newValueJson) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .entityName("Participant")
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
