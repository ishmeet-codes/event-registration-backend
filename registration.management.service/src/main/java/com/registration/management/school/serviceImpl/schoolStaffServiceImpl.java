package com.registration.management.school.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.registration.management.audit.entities.AuditLog;
import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.PasswordResetToken;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.PasswordResetTokenRepository;
import com.registration.management.auth.repository.RoleRepository;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.auth.service.EmailService;
import com.registration.management.enums.AuditAction;
import com.registration.management.enums.AuditStatus;
import com.registration.management.enums.StaffRole;
import com.registration.management.school.dto.schoolStaffDTO;
import com.registration.management.school.entity.School;
import com.registration.management.school.entity.SchoolStaff;
import com.registration.management.school.exception.SchoolNotActiveException;
import com.registration.management.school.exception.StaffConflictException;
import com.registration.management.school.exception.StaffRoleNotFoundException;
import com.registration.management.school.repository.schoolRepository;
import com.registration.management.school.repository.schoolStaffRepository;
import com.registration.management.school.service.schoolStaffService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class schoolStaffServiceImpl implements schoolStaffService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "fullName", "designation", "staffRole", "createdAt", "updatedAt", "active"
    );

    @Autowired
    private schoolRepository schoolRepository;

    @Autowired
    private schoolStaffRepository schoolStaffRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.mail.reset-password-base-url}")
    private String resetPasswordBaseUrl;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public schoolStaffDTO createStaff(Long schoolId, schoolStaffDTO request, User currentUser) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new EntityNotFoundException("School not found with id: " + schoolId));

        if (!school.isActive()) {
            throw new SchoolNotActiveException("School is not active with id: " + schoolId);
        }

        StaffRole role = request.getStaffRole();
        if (role == null) {
            throw new StaffRoleNotFoundException("Staff role is required");
        }

        validateRoleCapacityOnCreate(schoolId, role);

        final User resolvedUser = resolveCurrentUser(currentUser);

        SchoolStaff staff = modelMapper.map(request, SchoolStaff.class);
        staff.setSchool(school);
        staff.setStaffRole(role);
        staff.setActive(true);
        staff.setCreatedBy(resolvedUser);

        // Auto-provision User account if email is provided
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            User staffUser = userRepository.findByEmail(request.getEmail()).orElseGet(() -> {
                com.registration.management.auth.entities.Role userRole = roleRepository
                        .findByRoleCode(role.name())
                        .orElseGet(() -> roleRepository.findByRoleCode("SCHOOL_STAFF")
                        .orElseGet(() -> roleRepository.findByRoleCode("PARTICIPANT").orElse(null)));

                User newUser = User.builder()
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Random locked password
                        .fullName(request.getFullName())
                        .active(true)
                        .role(userRole)
                        .createdBy(resolvedUser)
                        .build();

                return userRepository.save(newUser);
            });
            
            staff.setUser(staffUser);
        }

        SchoolStaff savedStaff = schoolStaffRepository.save(staff);

        // Generate token and send welcome email if user was linked
        if (savedStaff.getUser() != null) {
            User staffUser = savedStaff.getUser();
            String token = UUID.randomUUID().toString();

            PasswordResetToken prt = PasswordResetToken.builder()
                    .user(staffUser)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(prt);

            String setPasswordLink = resetPasswordBaseUrl + "/reset-password?token=" + token;
            emailService.sendStaffWelcomeEmail(staffUser.getEmail(), staffUser.getFullName(), setPasswordLink);
        }

        schoolStaffDTO responseDto = toDto(savedStaff);

        try {
            String newValueJson = objectMapper.writeValueAsString(responseDto);
            AuditLog auditLog = AuditLog.builder()
                    .user(resolvedUser)
                    .entityName("SchoolStaff")
                    .entityId(savedStaff.getId())
                    .action(AuditAction.CREATE)
                    .status(AuditStatus.SUCCESS)
                    .newValue(newValueJson)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Do not break transaction if audit serialization fails
        }

        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<schoolStaffDTO> getStaffList(
            Long schoolId,
            String search,
            StaffRole staffRole,
            Boolean active,
            int page,
            int size,
            String sort
    ) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new EntityNotFoundException("School not found with id: " + schoolId);
        }

        int validPage = Math.max(0, page);
        int validSize = (size <= 0) ? 20 : Math.min(size, 100);

        Pageable pageable = createPageable(validPage, validSize, sort);
        Specification<SchoolStaff> spec = createStaffSpecification(schoolId, search, staffRole, active);

        Page<SchoolStaff> staffPage = schoolStaffRepository.findAll(spec, pageable);
        return staffPage.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public schoolStaffDTO getStaffById(Long schoolId, Long staffId) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new EntityNotFoundException("School not found with id: " + schoolId);
        }

        SchoolStaff staff = schoolStaffRepository.findByIdAndSchoolId(staffId, schoolId)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found with id: " + staffId + " for school: " + schoolId));

        return toDto(staff);
    }

    @Override
    public schoolStaffDTO updateStaff(Long schoolId, Long staffId, schoolStaffDTO request, User currentUser) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new EntityNotFoundException("School not found with id: " + schoolId);
        }

        SchoolStaff staff = schoolStaffRepository.findByIdAndSchoolId(staffId, schoolId)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found with id: " + staffId + " for school: " + schoolId));

        StaffRole newRole = request.getStaffRole() != null ? request.getStaffRole() : staff.getStaffRole();
        if (staff.isActive()) {
            validateRoleCapacityOnUpdate(schoolId, staffId, newRole);
        }

        final User resolvedUser = resolveCurrentUser(currentUser);

        schoolStaffDTO oldDto = toDto(staff);
        String oldValueJson = null;
        try {
            oldValueJson = objectMapper.writeValueAsString(oldDto);
        } catch (Exception e) {
            // Do not break transaction if old value serialization fails
        }

        staff.setFullName(request.getFullName());
        staff.setDesignation(request.getDesignation());
        staff.setPhone(request.getPhone());
        staff.setEmail(request.getEmail());
        staff.setStaffRole(newRole);
        staff.setUpdatedBy(resolvedUser);

        SchoolStaff savedStaff = schoolStaffRepository.save(staff);
        schoolStaffDTO responseDto = toDto(savedStaff);

        try {
            String newValueJson = objectMapper.writeValueAsString(responseDto);
            AuditLog auditLog = AuditLog.builder()
                    .user(resolvedUser)
                    .entityName("SchoolStaff")
                    .entityId(savedStaff.getId())
                    .action(AuditAction.UPDATE)
                    .status(AuditStatus.SUCCESS)
                    .oldValue(oldValueJson)
                    .newValue(newValueJson)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Do not break transaction if audit serialization fails
        }

        return responseDto;
    }

    @Override
    public schoolStaffDTO updateStaffStatus(Long schoolId, Long staffId, Boolean active, User currentUser) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new EntityNotFoundException("School not found with id: " + schoolId);
        }

        SchoolStaff staff = schoolStaffRepository.findByIdAndSchoolId(staffId, schoolId)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found with id: " + staffId + " for school: " + schoolId));

        final User resolvedUser = resolveCurrentUser(currentUser);
        boolean isSuperAdmin = resolvedUser != null && resolvedUser.getRole() != null && "SUPER_ADMIN".equals(resolvedUser.getRole().getRoleCode());

        boolean newActiveStatus = Boolean.TRUE.equals(active);

        if (staff.isActive() && !newActiveStatus) {
            // Deactivating
            if (!isSuperAdmin && staff.getStaffRole() == StaffRole.LOGIN_TEACHER) {
                long otherActiveLoginTeachers = schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(
                        schoolId, StaffRole.LOGIN_TEACHER, staffId);
                if (otherActiveLoginTeachers == 0) {
                    throw new StaffConflictException("Cannot deactivate the only active LOGIN_TEACHER for school: " + schoolId);
                }
            }
        } else if (!staff.isActive() && newActiveStatus) {
            // Activating
            validateRoleCapacityOnUpdate(schoolId, staffId, staff.getStaffRole());
        }

        schoolStaffDTO oldDto = toDto(staff);
        String oldValueJson = null;
        try {
            oldValueJson = objectMapper.writeValueAsString(oldDto);
        } catch (Exception e) {
            // Do not break transaction if old value serialization fails
        }

        staff.setActive(newActiveStatus);
        staff.setUpdatedBy(resolvedUser);

        SchoolStaff savedStaff = schoolStaffRepository.save(staff);
        schoolStaffDTO responseDto = toDto(savedStaff);

        try {
            String newValueJson = objectMapper.writeValueAsString(responseDto);
            AuditLog auditLog = AuditLog.builder()
                    .user(resolvedUser)
                    .entityName("SchoolStaff")
                    .entityId(savedStaff.getId())
                    .action(AuditAction.UPDATE)
                    .status(AuditStatus.SUCCESS)
                    .oldValue(oldValueJson)
                    .newValue(newValueJson)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Do not break transaction if audit serialization fails
        }

        return responseDto;
    }

    @Override
    public void deleteStaff(Long schoolId, Long staffId, User currentUser) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new EntityNotFoundException("School not found with id: " + schoolId);
        }

        SchoolStaff staff = schoolStaffRepository.findByIdAndSchoolId(staffId, schoolId)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found with id: " + staffId + " for school: " + schoolId));

        final User resolvedUser = resolveCurrentUser(currentUser);
        boolean isSuperAdmin = resolvedUser != null && resolvedUser.getRole() != null && "SUPER_ADMIN".equals(resolvedUser.getRole().getRoleCode());

        if (!isSuperAdmin && staff.isActive() && staff.getStaffRole() == StaffRole.LOGIN_TEACHER) {
            long otherActiveLoginTeachers = schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(
                    schoolId, StaffRole.LOGIN_TEACHER, staffId);
            if (otherActiveLoginTeachers == 0) {
                throw new StaffConflictException("Cannot delete the only active LOGIN_TEACHER for school: " + schoolId);
            }
        }

        schoolStaffDTO oldDto = toDto(staff);
        String oldValueJson = null;
        try {
            oldValueJson = objectMapper.writeValueAsString(oldDto);
        } catch (Exception e) {
            // Do not break transaction if old value serialization fails
        }

        staff.setActive(false);
        staff.setUpdatedBy(resolvedUser);

        SchoolStaff savedStaff = schoolStaffRepository.save(staff);
        schoolStaffDTO responseDto = toDto(savedStaff);

        try {
            String newValueJson = objectMapper.writeValueAsString(responseDto);
            AuditLog auditLog = AuditLog.builder()
                    .user(resolvedUser)
                    .entityName("SchoolStaff")
                    .entityId(savedStaff.getId())
                    .action(AuditAction.DELETE)
                    .status(AuditStatus.SUCCESS)
                    .oldValue(oldValueJson)
                    .newValue(newValueJson)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Do not break transaction if audit serialization fails
        }
    }

    private void validateRoleCapacityOnCreate(Long schoolId, StaffRole role) {
        if (role == StaffRole.LOGIN_TEACHER) {
            long count = schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrue(schoolId, StaffRole.LOGIN_TEACHER);
            if (count >= 1) {
                throw new StaffConflictException("A LOGIN_TEACHER already exists for school: " + schoolId);
            }
        } else if (role == StaffRole.ACCOMPANYING_TEACHER) {
            long count = schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrue(schoolId, StaffRole.ACCOMPANYING_TEACHER);
            if (count >= 5) {
                throw new StaffConflictException("Maximum limit of 5 accompanying teachers reached for school: " + schoolId);
            }
        }
    }

    private void validateRoleCapacityOnUpdate(Long schoolId, Long staffId, StaffRole newRole) {
        if (newRole == StaffRole.LOGIN_TEACHER) {
            long count = schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(
                    schoolId, StaffRole.LOGIN_TEACHER, staffId);
            if (count >= 1) {
                throw new StaffConflictException("A LOGIN_TEACHER already exists for school: " + schoolId);
            }
        } else if (newRole == StaffRole.ACCOMPANYING_TEACHER) {
            long count = schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(
                    schoolId, StaffRole.ACCOMPANYING_TEACHER, staffId);
            if (count >= 5) {
                throw new StaffConflictException("Maximum limit of 5 accompanying teachers reached for school: " + schoolId);
            }
        }
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
            sort = Sort.by(Sort.Direction.ASC, "fullName");
        }
        return PageRequest.of(page, size, sort);
    }

    private Specification<SchoolStaff> createStaffSpecification(
            Long schoolId,
            String search,
            StaffRole staffRole,
            Boolean active
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("school").get("id"), schoolId));

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (staffRole != null) {
                predicates.add(cb.equal(root.get("staffRole"), staffRole));
            }

            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("fullName")), searchPattern),
                        cb.like(cb.lower(root.get("designation")), searchPattern),
                        cb.like(cb.lower(root.get("phone")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private schoolStaffDTO toDto(SchoolStaff staff) {
        if (staff == null) {
            return null;
        }
        schoolStaffDTO dto = modelMapper.map(staff, schoolStaffDTO.class);
        if (staff.getSchool() != null) {
            dto.setSchoolId(staff.getSchool().getId());
        }
        if (staff.getCreatedBy() != null) {
            dto.setCreatedById(staff.getCreatedBy().getId());
            dto.setCreatedByName(staff.getCreatedBy().getFullName());
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
