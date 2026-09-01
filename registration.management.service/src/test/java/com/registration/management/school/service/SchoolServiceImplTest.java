package com.registration.management.school.service;

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
import com.registration.management.school.serviceImpl.schoolServiceImpl;
import com.registration.management.school.util.SchoolTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolServiceImplTest {

    @Mock
    private schoolRepository schoolRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private ModelMapper modelMapper = createModelMapper();

    @InjectMocks
    private schoolServiceImpl schoolService;

    private User currentUser;
    private schoolDTO requestDto;

    private static ModelMapper createModelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper;
    }

    @BeforeEach
    void setUp() {
        currentUser = SchoolTestDataFactory.createTestUser();
        requestDto = SchoolTestDataFactory.createValidSchoolDTO();
    }

    @Test
    void createSchool_Success() {
        when(schoolRepository.existsBySchoolCode("SCH001")).thenReturn(false);

        School savedSchool = SchoolTestDataFactory.createSchoolEntity(currentUser);
        when(schoolRepository.save(any(School.class))).thenReturn(savedSchool);

        schoolDTO result = schoolService.createSchool(requestDto, currentUser);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("SCH001", result.getSchoolCode());
        assertEquals("ABC Public School", result.getSchoolName());
        assertTrue(result.getActive());
        assertEquals(10L, result.getCreatedById());
        assertEquals("Test Admin", result.getCreatedByName());

        // Verify entity passed to repository.save
        ArgumentCaptor<School> schoolCaptor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(schoolCaptor.capture());
        School entityToSave = schoolCaptor.getValue();
        assertTrue(entityToSave.isActive());
        assertEquals(currentUser, entityToSave.getCreatedBy());

        // Verify AuditLog creation
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog auditLog = auditCaptor.getValue();
        assertEquals("School", auditLog.getEntityName());
        assertEquals(100L, auditLog.getEntityId());
        assertEquals(AuditAction.CREATE, auditLog.getAction());
        assertEquals(AuditStatus.SUCCESS, auditLog.getStatus());
        assertEquals(currentUser, auditLog.getUser());
    }

    @Test
    void createSchool_DuplicateCode_ThrowsSchoolCodeException() {
        when(schoolRepository.existsBySchoolCode("SCH001")).thenReturn(true);

        SchoolCodeException exception = assertThrows(
                SchoolCodeException.class,
                () -> schoolService.createSchool(requestDto, currentUser)
        );

        assertTrue(exception.getMessage().contains("School code already exists: SCH001"));
        verify(schoolRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void getSchools_Success() {
        School school = SchoolTestDataFactory.createSchoolEntity(currentUser);
        Page<School> schoolPage = new PageImpl<>(List.of(school));

        when(schoolRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(schoolPage);

        Page<schoolDTO> result = schoolService.getSchools("ABC", "Ludhiana", "Ludhiana", "Punjab", "CBSE", true, 0, 20, "schoolName,asc");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("SCH001", result.getContent().get(0).getSchoolCode());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(schoolRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals("schoolName: ASC", pageable.getSort().toString());
    }

    @Test
    void getSchools_CapsPageSizeTo100() {
        Page<School> emptyPage = new PageImpl<>(List.of());
        when(schoolRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        schoolService.getSchools(null, null, null, null, null, null, 0, 250, "createdAt,desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(schoolRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(100, pageable.getPageSize());
        assertEquals("createdAt: DESC", pageable.getSort().toString());
    }

    @Test
    void getSchools_InvalidSortField_FallsBackToDefaultSort() {
        Page<School> emptyPage = new PageImpl<>(List.of());
        when(schoolRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        schoolService.getSchools(null, null, null, null, null, null, 0, 20, "unauthorizedColumn,asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(schoolRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals("schoolName: ASC", pageable.getSort().toString());
    }
}
