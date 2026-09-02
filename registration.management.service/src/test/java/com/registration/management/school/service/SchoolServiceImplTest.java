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

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

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

    @Test
    void getSchoolById_Success_WithoutSummary() {
        School school = SchoolTestDataFactory.createSchoolEntity(currentUser);
        when(schoolRepository.findById(100L)).thenReturn(Optional.of(school));

        schoolDTO result = schoolService.getSchoolById(100L, false);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("SCH001", result.getSchoolCode());
        assertEquals("ABC Public School", result.getSchoolName());
        assertNull(result.getStaffCount());
        assertNull(result.getRegistrationCount());

        verify(schoolRepository, never()).countStaffBySchoolId(anyLong());
        verify(schoolRepository, never()).countRegistrationsBySchoolId(anyLong());
    }

    @Test
    void getSchoolById_Success_WithSummary() {
        School school = SchoolTestDataFactory.createSchoolEntity(currentUser);
        when(schoolRepository.findById(100L)).thenReturn(Optional.of(school));
        when(schoolRepository.countStaffBySchoolId(100L)).thenReturn(4L);
        when(schoolRepository.countRegistrationsBySchoolId(100L)).thenReturn(3L);

        schoolDTO result = schoolService.getSchoolById(100L, true);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("SCH001", result.getSchoolCode());
        assertEquals(4L, result.getStaffCount());
        assertEquals(3L, result.getRegistrationCount());

        verify(schoolRepository).countStaffBySchoolId(100L);
        verify(schoolRepository).countRegistrationsBySchoolId(100L);
    }

    @Test
    void getSchoolById_NotFound_ThrowsEntityNotFoundException() {
        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> schoolService.getSchoolById(999L, false)
        );

        assertTrue(ex.getMessage().contains("School not found with id: 999"));
    }

    @Test
    void updateSchool_Success() {
        School existingSchool = SchoolTestDataFactory.createSchoolEntity(currentUser);
        when(schoolRepository.findById(100L)).thenReturn(Optional.of(existingSchool));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));

        schoolDTO updateRequest = SchoolTestDataFactory.createValidSchoolDTO();
        updateRequest.setSchoolName("Updated Public School");
        updateRequest.setCity("Amritsar");

        schoolDTO result = schoolService.updateSchool(100L, updateRequest, currentUser);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Updated Public School", result.getSchoolName());
        assertEquals("Amritsar", result.getCity());

        ArgumentCaptor<School> schoolCaptor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(schoolCaptor.capture());
        School savedEntity = schoolCaptor.getValue();
        assertEquals(100L, savedEntity.getId());
        assertEquals("Updated Public School", savedEntity.getSchoolName());
        assertEquals("Amritsar", savedEntity.getCity());
        assertEquals(currentUser, savedEntity.getCreatedBy()); // createdBy preserved
        assertEquals(currentUser, savedEntity.getUpdatedBy()); // updatedBy set

        // Verify AuditLog for UPDATE
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog auditLog = auditCaptor.getValue();
        assertEquals("School", auditLog.getEntityName());
        assertEquals(100L, auditLog.getEntityId());
        assertEquals(AuditAction.UPDATE, auditLog.getAction());
        assertEquals(AuditStatus.SUCCESS, auditLog.getStatus());
        assertEquals(currentUser, auditLog.getUser());
        assertNotNull(auditLog.getOldValue());
        assertNotNull(auditLog.getNewValue());
        assertTrue(auditLog.getOldValue().contains("ABC Public School"));
        assertTrue(auditLog.getNewValue().contains("Updated Public School"));
    }

    @Test
    void updateSchool_DuplicateSchoolCode_ThrowsSchoolCodeException() {
        School existingSchool = SchoolTestDataFactory.createSchoolEntity(currentUser);
        when(schoolRepository.findById(100L)).thenReturn(Optional.of(existingSchool));
        when(schoolRepository.existsBySchoolCode("SCH999")).thenReturn(true);

        schoolDTO updateRequest = SchoolTestDataFactory.createValidSchoolDTO();
        updateRequest.setSchoolCode("SCH999");

        SchoolCodeException ex = assertThrows(
                SchoolCodeException.class,
                () -> schoolService.updateSchool(100L, updateRequest, currentUser)
        );

        assertTrue(ex.getMessage().contains("School code already exists: SCH999"));
        verify(schoolRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void updateSchool_SameSchoolCode_DoesNotCheckUniqueness() {
        School existingSchool = SchoolTestDataFactory.createSchoolEntity(currentUser);
        when(schoolRepository.findById(100L)).thenReturn(Optional.of(existingSchool));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));

        schoolDTO updateRequest = SchoolTestDataFactory.createValidSchoolDTO();
        updateRequest.setSchoolCode("SCH001"); // same code
        updateRequest.setSchoolName("New Name");

        schoolDTO result = schoolService.updateSchool(100L, updateRequest, currentUser);

        assertNotNull(result);
        assertEquals("New Name", result.getSchoolName());
        verify(schoolRepository, never()).existsBySchoolCode("SCH001");
    }

    @Test
    void updateSchool_NotFound_ThrowsEntityNotFoundException() {
        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        schoolDTO updateRequest = SchoolTestDataFactory.createValidSchoolDTO();

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> schoolService.updateSchool(999L, updateRequest, currentUser)
        );

        assertTrue(ex.getMessage().contains("School not found with id: 999"));
        verify(schoolRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void updateSchoolStatus_Deactivate_Success() {
        School existingSchool = SchoolTestDataFactory.createSchoolEntity(currentUser);
        existingSchool.setActive(true);
        when(schoolRepository.findById(100L)).thenReturn(Optional.of(existingSchool));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));

        schoolDTO result = schoolService.updateSchoolStatus(100L, false, currentUser);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertFalse(result.getActive());

        ArgumentCaptor<School> schoolCaptor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(schoolCaptor.capture());
        School savedSchool = schoolCaptor.getValue();
        assertEquals(100L, savedSchool.getId());
        assertFalse(savedSchool.isActive());
        assertEquals(currentUser, savedSchool.getCreatedBy()); // createdBy preserved
        assertEquals(currentUser, savedSchool.getUpdatedBy()); // updatedBy set

        // Verify AuditLog for UPDATE
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog auditLog = auditCaptor.getValue();
        assertEquals("School", auditLog.getEntityName());
        assertEquals(100L, auditLog.getEntityId());
        assertEquals(AuditAction.UPDATE, auditLog.getAction());
        assertEquals(AuditStatus.SUCCESS, auditLog.getStatus());
        assertEquals(currentUser, auditLog.getUser());
        assertNotNull(auditLog.getOldValue());
        assertNotNull(auditLog.getNewValue());
        assertTrue(auditLog.getOldValue().contains("\"active\":true"));
        assertTrue(auditLog.getNewValue().contains("\"active\":false"));
    }

    @Test
    void updateSchoolStatus_Activate_Success() {
        School existingSchool = SchoolTestDataFactory.createSchoolEntity(currentUser);
        existingSchool.setActive(false);
        when(schoolRepository.findById(100L)).thenReturn(Optional.of(existingSchool));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));

        schoolDTO result = schoolService.updateSchoolStatus(100L, true, currentUser);

        assertNotNull(result);
        assertTrue(result.getActive());
    }

    @Test
    void updateSchoolStatus_NotFound_ThrowsEntityNotFoundException() {
        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> schoolService.updateSchoolStatus(999L, false, currentUser)
        );

        assertTrue(ex.getMessage().contains("School not found with id: 999"));
        verify(schoolRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }
}
