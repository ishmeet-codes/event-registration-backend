package com.registration.management.school.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.registration.management.audit.entities.AuditLog;
import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
import com.registration.management.school.dto.schoolDTO;
import com.registration.management.school.entity.School;
import com.registration.management.school.exception.SchoolCodeException;
import com.registration.management.school.repository.schoolRepository;
import com.registration.management.school.service.schoolService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class schoolServiceImpl implements schoolService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "schoolCode", "schoolName", "principalName", "city", "district", "state", "createdAt", "updatedAt"
    );

    @Autowired
    private schoolRepository schoolRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public schoolDTO createSchool(schoolDTO request, User currentUser) {
        // 1. Validate school code uniqueness
        if (schoolRepository.existsBySchoolCode(request.getSchoolCode())) {
            throw new SchoolCodeException("School code already exists: " + request.getSchoolCode());
        }

        // 2. Resolve authenticated user if not provided directly
        currentUser = resolveCurrentUser(currentUser);

        // 3. Map request DTO to School entity using ModelMapper & set system attributes
        School school = modelMapper.map(request, School.class);
        school.setActive(true);
        school.setCreatedBy(currentUser);

        School savedSchool = schoolRepository.save(school);
        schoolDTO responseDto = toDto(savedSchool);

        // 4. Audit CREATE
        try {
            String newValueJson = objectMapper.writeValueAsString(responseDto);
            AuditLog auditLog = AuditLog.builder()
                    .user(currentUser)
                    .entityName("School")
                    .entityId(savedSchool.getId())
                    .action(AuditAction.CREATE)
                    .status(AuditStatus.SUCCESS)
                    .newValue(newValueJson)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Do not break school creation transaction if audit serialization fails
        }

        return responseDto;
    }

    @Override
    public schoolDTO updateSchool(Long schoolId, schoolDTO request, User currentUser) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new EntityNotFoundException("School not found with id: " + schoolId));

        if (request.getSchoolCode() != null && !request.getSchoolCode().equals(school.getSchoolCode())) {
            if (schoolRepository.existsBySchoolCode(request.getSchoolCode())) {
                throw new SchoolCodeException("School code already exists: " + request.getSchoolCode());
            }
        }

        currentUser = resolveCurrentUser(currentUser);

        schoolDTO oldDto = toDto(school);
        String oldValueJson = null;
        try {
            oldValueJson = objectMapper.writeValueAsString(oldDto);
        } catch (Exception e) {
            // Do not break transaction if old value serialization fails
        }

        school.setSchoolCode(request.getSchoolCode());
        school.setSchoolName(request.getSchoolName());
        school.setPrincipalName(request.getPrincipalName());
        school.setBoard(request.getBoard());
        school.setAddress(request.getAddress());
        school.setCity(request.getCity());
        school.setDistrict(request.getDistrict());
        school.setState(request.getState());
        school.setPincode(request.getPincode());
        school.setPhone(request.getPhone());
        school.setEmail(request.getEmail());
        if (request.getActive() != null) {
            school.setActive(request.getActive());
        }
        school.setUpdatedBy(currentUser);

        School savedSchool = schoolRepository.save(school);
        schoolDTO responseDto = toDto(savedSchool);

        try {
            String newValueJson = objectMapper.writeValueAsString(responseDto);
            AuditLog auditLog = AuditLog.builder()
                    .user(currentUser)
                    .entityName("School")
                    .entityId(savedSchool.getId())
                    .action(AuditAction.UPDATE)
                    .status(AuditStatus.SUCCESS)
                    .oldValue(oldValueJson)
                    .newValue(newValueJson)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Do not break school update transaction if audit serialization fails
        }

        return responseDto;
    }

    @Override
    public schoolDTO updateSchoolStatus(Long schoolId, Boolean active, User currentUser) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new EntityNotFoundException("School not found with id: " + schoolId));

        currentUser = resolveCurrentUser(currentUser);

        schoolDTO oldDto = toDto(school);
        String oldValueJson = null;
        try {
            oldValueJson = objectMapper.writeValueAsString(oldDto);
        } catch (Exception e) {
            // Do not break transaction if old value serialization fails
        }

        school.setActive(Boolean.TRUE.equals(active));
        school.setUpdatedBy(currentUser);

        School savedSchool = schoolRepository.save(school);
        schoolDTO responseDto = toDto(savedSchool);

        try {
            String newValueJson = objectMapper.writeValueAsString(responseDto);
            AuditLog auditLog = AuditLog.builder()
                    .user(currentUser)
                    .entityName("School")
                    .entityId(savedSchool.getId())
                    .action(AuditAction.UPDATE)
                    .status(AuditStatus.SUCCESS)
                    .oldValue(oldValueJson)
                    .newValue(newValueJson)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Do not break school status update transaction if audit serialization fails
        }

        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public schoolDTO getSchoolById(Long schoolId, boolean includeSummary) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new EntityNotFoundException("School not found with id: " + schoolId));

        schoolDTO dto = toDto(school);
        if (includeSummary) {
            long staffCount = schoolRepository.countStaffBySchoolId(schoolId);
            long registrationCount = schoolRepository.countRegistrationsBySchoolId(schoolId);
            dto.setStaffCount(staffCount);
            dto.setRegistrationCount(registrationCount);
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<schoolDTO> getSchools(
            String search,
            String city,
            String district,
            String state,
            String board,
            Boolean active,
            int page,
            int size,
            String sort
    ) {
        int validPage = Math.max(0, page);
        int validSize = (size <= 0) ? 20 : Math.min(size, 100);

        Pageable pageable = createPageable(validPage, validSize, sort);
        Specification<School> spec = createSchoolSpecification(search, city, district, state, board, active);

        Page<School> schoolPage = schoolRepository.findAll(spec, pageable);
        return schoolPage.map(this::toDto);
    }

    private Pageable createPageable(int page, int size, String sortParam) {
        Sort sort = Sort.unsorted();
        if (sortParam != null && !sortParam.isBlank()) {
            String[] parts = sortParam.split(",");
            String property = parts[0].trim();
            if (ALLOWED_SORT_FIELDS.contains(property)) {
                Sort.Direction direction = Sort.Direction.ASC;
                if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                    direction = Sort.Direction.DESC;
                }
                sort = Sort.by(direction, property);
            }
        }
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.ASC, "schoolName");
        }
        return PageRequest.of(page, size, sort);
    }

    private Specification<School> createSchoolSpecification(
            String search,
            String city,
            String district,
            String state,
            String board,
            Boolean active
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase()));
            }

            if (district != null && !district.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("district")), district.trim().toLowerCase()));
            }

            if (state != null && !state.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("state")), state.trim().toLowerCase()));
            }

            if (board != null && !board.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("board")), board.trim().toLowerCase()));
            }

            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("schoolCode")), searchPattern),
                        cb.like(cb.lower(root.get("schoolName")), searchPattern),
                        cb.like(cb.lower(root.get("principalName")), searchPattern),
                        cb.like(cb.lower(root.get("city")), searchPattern),
                        cb.like(cb.lower(root.get("district")), searchPattern),
                        cb.like(cb.lower(root.get("state")), searchPattern),
                        cb.like(cb.lower(root.get("phone")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private schoolDTO toDto(School school) {
        schoolDTO dto = modelMapper.map(school, schoolDTO.class);
        if (school.getCreatedBy() != null) {
            dto.setCreatedById(school.getCreatedBy().getId());
            dto.setCreatedByName(school.getCreatedBy().getFullName());
        }
        return dto;
    }

    private User resolveCurrentUser(User currentUser) {
        if (currentUser == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String email = authentication.getName();
                currentUser = userRepository.findByEmail(email).orElse(null);
            }
        }
        return currentUser;
    }
}
