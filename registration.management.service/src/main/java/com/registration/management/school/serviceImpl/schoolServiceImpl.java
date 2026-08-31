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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class schoolServiceImpl implements schoolService {

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
        if (currentUser == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String email = authentication.getName();
                currentUser = userRepository.findByEmail(email).orElse(null);
            }
        }

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

    private schoolDTO toDto(School school) {
        schoolDTO dto = modelMapper.map(school, schoolDTO.class);
        if (school.getCreatedBy() != null) {
            dto.setCreatedById(school.getCreatedBy().getId());
            dto.setCreatedByName(school.getCreatedBy().getFullName());
        }
        return dto;
    }
}
