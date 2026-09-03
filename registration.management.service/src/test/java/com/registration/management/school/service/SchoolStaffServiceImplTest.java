package com.registration.management.school.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.registration.management.audit.entities.AuditLog;
import com.registration.management.audit.repository.AuditLogRepository;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.enums.StaffRole;
import com.registration.management.school.dto.schoolStaffDTO;
import com.registration.management.school.entity.School;
import com.registration.management.school.entity.SchoolStaff;
import com.registration.management.school.exception.SchoolNotActiveException;
import com.registration.management.school.exception.StaffConflictException;
import com.registration.management.school.exception.StaffRoleNotFoundException;
import com.registration.management.school.repository.schoolRepository;
import com.registration.management.school.repository.schoolStaffRepository;
import com.registration.management.school.serviceImpl.schoolStaffServiceImpl;
import com.registration.management.school.util.SchoolStaffTestDataFactory;
import com.registration.management.school.util.SchoolTestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolStaffServiceImplTest {

    @Mock
    private schoolRepository schoolRepository;

    @Mock
    private schoolStaffRepository schoolStaffRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private ModelMapper modelMapper = createModelMapper();

    @InjectMocks
    private schoolStaffServiceImpl schoolStaffService;

    private static ModelMapper createModelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper;
    }

    private User currentUser;
    private School activeSchool;
    private School inactiveSchool;

    @BeforeEach
    void setUp() {
        currentUser = SchoolTestDataFactory.createTestUser();
        activeSchool = SchoolTestDataFactory.createSchoolEntity(currentUser);
        activeSchool.setId(1L);
        activeSchool.setActive(true);

        inactiveSchool = SchoolTestDataFactory.createSchoolEntity(currentUser);
        inactiveSchool.setId(2L);
        inactiveSchool.setActive(false);
    }

    @Test
    void createStaff_Success_AccompanyingTeacher() {
        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.ACCOMPANYING_TEACHER);

        when(schoolRepository.findById(1L)).thenReturn(Optional.of(activeSchool));
        when(schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrue(1L, StaffRole.ACCOMPANYING_TEACHER)).thenReturn(2L);

        SchoolStaff savedEntity = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.ACCOMPANYING_TEACHER, true, currentUser);
        when(schoolStaffRepository.save(any(SchoolStaff.class))).thenReturn(savedEntity);

        schoolStaffDTO result = schoolStaffService.createStaff(1L, request, currentUser);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getSchoolId());
        assertEquals("Aman Sharma", result.getFullName());
        assertEquals(StaffRole.ACCOMPANYING_TEACHER, result.getStaffRole());
        assertTrue(result.getActive());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void createStaff_Success_LoginTeacher() {
        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.LOGIN_TEACHER);

        when(schoolRepository.findById(1L)).thenReturn(Optional.of(activeSchool));
        when(schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrue(1L, StaffRole.LOGIN_TEACHER)).thenReturn(0L);

        SchoolStaff savedEntity = SchoolStaffTestDataFactory.createStaffEntity(11L, activeSchool, StaffRole.LOGIN_TEACHER, true, currentUser);
        when(schoolStaffRepository.save(any(SchoolStaff.class))).thenReturn(savedEntity);

        schoolStaffDTO result = schoolStaffService.createStaff(1L, request, currentUser);

        assertNotNull(result);
        assertEquals(11L, result.getId());
        assertEquals(StaffRole.LOGIN_TEACHER, result.getStaffRole());
        verify(schoolStaffRepository).save(any(SchoolStaff.class));
    }

    @Test
    void createStaff_SchoolNotFound_ThrowsEntityNotFoundException() {
        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.ACCOMPANYING_TEACHER);

        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> schoolStaffService.createStaff(999L, request, currentUser));

        assertTrue(ex.getMessage().contains("School not found with id: 999"));
        verify(schoolStaffRepository, never()).save(any());
    }

    @Test
    void createStaff_SchoolInactive_ThrowsSchoolNotActiveException() {
        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.ACCOMPANYING_TEACHER);

        when(schoolRepository.findById(2L)).thenReturn(Optional.of(inactiveSchool));

        SchoolNotActiveException ex = assertThrows(SchoolNotActiveException.class,
                () -> schoolStaffService.createStaff(2L, request, currentUser));

        assertTrue(ex.getMessage().contains("School is not active"));
        verify(schoolStaffRepository, never()).save(any());
    }

    @Test
    void createStaff_StaffRoleNull_ThrowsStaffRoleNotFoundException() {
        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.ACCOMPANYING_TEACHER);
        request.setStaffRole(null);

        when(schoolRepository.findById(1L)).thenReturn(Optional.of(activeSchool));

        StaffRoleNotFoundException ex = assertThrows(StaffRoleNotFoundException.class,
                () -> schoolStaffService.createStaff(1L, request, currentUser));

        assertTrue(ex.getMessage().contains("Staff role is required"));
        verify(schoolStaffRepository, never()).save(any());
    }

    @Test
    void createStaff_LoginTeacherAlreadyExists_ThrowsStaffConflictException() {
        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.LOGIN_TEACHER);

        when(schoolRepository.findById(1L)).thenReturn(Optional.of(activeSchool));
        when(schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrue(1L, StaffRole.LOGIN_TEACHER)).thenReturn(1L);

        StaffConflictException ex = assertThrows(StaffConflictException.class,
                () -> schoolStaffService.createStaff(1L, request, currentUser));

        assertTrue(ex.getMessage().contains("A LOGIN_TEACHER already exists"));
        verify(schoolStaffRepository, never()).save(any());
    }

    @Test
    void createStaff_AccompanyingTeacherLimitExceeded_ThrowsStaffConflictException() {
        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.ACCOMPANYING_TEACHER);

        when(schoolRepository.findById(1L)).thenReturn(Optional.of(activeSchool));
        when(schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrue(1L, StaffRole.ACCOMPANYING_TEACHER)).thenReturn(5L);

        StaffConflictException ex = assertThrows(StaffConflictException.class,
                () -> schoolStaffService.createStaff(1L, request, currentUser));

        assertTrue(ex.getMessage().contains("Maximum limit of 5 accompanying teachers reached"));
        verify(schoolStaffRepository, never()).save(any());
    }

    @Test
    void getStaffList_Success() {
        when(schoolRepository.existsById(1L)).thenReturn(true);

        SchoolStaff staff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.ACCOMPANYING_TEACHER, true, currentUser);
        Page<SchoolStaff> page = new PageImpl<>(List.of(staff));

        when(schoolStaffRepository.findAll(ArgumentMatchers.<Specification<SchoolStaff>>any(), any(Pageable.class)))
                .thenReturn(page);

        Page<schoolStaffDTO> result = schoolStaffService.getStaffList(1L, "Aman", StaffRole.ACCOMPANYING_TEACHER, true, 0, 10, "fullName,asc");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Aman Sharma", result.getContent().get(0).getFullName());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(schoolStaffRepository).findAll(ArgumentMatchers.<Specification<SchoolStaff>>any(), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getStaffList_SchoolNotFound_ThrowsEntityNotFoundException() {
        when(schoolRepository.existsById(999L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class,
                () -> schoolStaffService.getStaffList(999L, null, null, null, 0, 20, "fullName,asc"));
    }

    @Test
    void getStaffById_Success() {
        when(schoolRepository.existsById(1L)).thenReturn(true);

        SchoolStaff staff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.ACCOMPANYING_TEACHER, true, currentUser);
        when(schoolStaffRepository.findByIdAndSchoolId(10L, 1L)).thenReturn(Optional.of(staff));

        schoolStaffDTO result = schoolStaffService.getStaffById(1L, 10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getSchoolId());
    }

    @Test
    void getStaffById_StaffNotBelongingToSchool_ThrowsEntityNotFoundException() {
        when(schoolRepository.existsById(1L)).thenReturn(true);
        when(schoolStaffRepository.findByIdAndSchoolId(999L, 1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> schoolStaffService.getStaffById(1L, 999L));

        assertTrue(ex.getMessage().contains("Staff not found with id: 999 for school: 1"));
    }

    @Test
    void updateStaff_Success() {
        when(schoolRepository.existsById(1L)).thenReturn(true);

        SchoolStaff staff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.ACCOMPANYING_TEACHER, true, currentUser);
        when(schoolStaffRepository.findByIdAndSchoolId(10L, 1L)).thenReturn(Optional.of(staff));
        when(schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(1L, StaffRole.ACCOMPANYING_TEACHER, 10L)).thenReturn(2L);

        SchoolStaff updatedStaff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.ACCOMPANYING_TEACHER, true, currentUser);
        updatedStaff.setFullName("Updated Aman");
        when(schoolStaffRepository.save(any(SchoolStaff.class))).thenReturn(updatedStaff);

        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.ACCOMPANYING_TEACHER);
        request.setFullName("Updated Aman");

        schoolStaffDTO result = schoolStaffService.updateStaff(1L, 10L, request, currentUser);

        assertNotNull(result);
        assertEquals("Updated Aman", result.getFullName());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void updateStaff_ChangeToLoginTeacher_WhenAlreadyExists_ThrowsStaffConflictException() {
        when(schoolRepository.existsById(1L)).thenReturn(true);

        SchoolStaff staff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.ACCOMPANYING_TEACHER, true, currentUser);
        when(schoolStaffRepository.findByIdAndSchoolId(10L, 1L)).thenReturn(Optional.of(staff));
        when(schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(1L, StaffRole.LOGIN_TEACHER, 10L)).thenReturn(1L);

        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.LOGIN_TEACHER);

        StaffConflictException ex = assertThrows(StaffConflictException.class,
                () -> schoolStaffService.updateStaff(1L, 10L, request, currentUser));

        assertTrue(ex.getMessage().contains("A LOGIN_TEACHER already exists"));
        verify(schoolStaffRepository, never()).save(any());
    }

    @Test
    void updateStaff_ChangeToAccompanying_WhenLimitReached_ThrowsStaffConflictException() {
        when(schoolRepository.existsById(1L)).thenReturn(true);

        SchoolStaff staff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.LOGIN_TEACHER, true, currentUser);
        when(schoolStaffRepository.findByIdAndSchoolId(10L, 1L)).thenReturn(Optional.of(staff));
        when(schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(1L, StaffRole.ACCOMPANYING_TEACHER, 10L)).thenReturn(5L);

        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.ACCOMPANYING_TEACHER);

        StaffConflictException ex = assertThrows(StaffConflictException.class,
                () -> schoolStaffService.updateStaff(1L, 10L, request, currentUser));

        assertTrue(ex.getMessage().contains("Maximum limit of 5 accompanying teachers reached"));
        verify(schoolStaffRepository, never()).save(any());
    }

    @Test
    void updateStaffStatus_DeactivateOnlyLoginTeacher_ThrowsStaffConflictException() {
        when(schoolRepository.existsById(1L)).thenReturn(true);

        SchoolStaff staff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.LOGIN_TEACHER, true, currentUser);
        when(schoolStaffRepository.findByIdAndSchoolId(10L, 1L)).thenReturn(Optional.of(staff));
        when(schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(1L, StaffRole.LOGIN_TEACHER, 10L)).thenReturn(0L);

        StaffConflictException ex = assertThrows(StaffConflictException.class,
                () -> schoolStaffService.updateStaffStatus(1L, 10L, false, currentUser));

        assertTrue(ex.getMessage().contains("Cannot deactivate the only active LOGIN_TEACHER"));
        verify(schoolStaffRepository, never()).save(any());
    }

    @Test
    void updateStaffStatus_DeactivateLoginTeacher_WhenAnotherExists_Success() {
        when(schoolRepository.existsById(1L)).thenReturn(true);

        SchoolStaff staff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.LOGIN_TEACHER, true, currentUser);
        when(schoolStaffRepository.findByIdAndSchoolId(10L, 1L)).thenReturn(Optional.of(staff));
        when(schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(1L, StaffRole.LOGIN_TEACHER, 10L)).thenReturn(1L);

        SchoolStaff deactivatedStaff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.LOGIN_TEACHER, false, currentUser);
        when(schoolStaffRepository.save(any(SchoolStaff.class))).thenReturn(deactivatedStaff);

        schoolStaffDTO result = schoolStaffService.updateStaffStatus(1L, 10L, false, currentUser);

        assertNotNull(result);
        assertFalse(result.getActive());
        verify(schoolStaffRepository).save(any(SchoolStaff.class));
    }

    @Test
    void updateStaffStatus_ActivateAccompanying_WhenLimitReached_ThrowsStaffConflictException() {
        when(schoolRepository.existsById(1L)).thenReturn(true);

        SchoolStaff staff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.ACCOMPANYING_TEACHER, false, currentUser);
        when(schoolStaffRepository.findByIdAndSchoolId(10L, 1L)).thenReturn(Optional.of(staff));
        when(schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(1L, StaffRole.ACCOMPANYING_TEACHER, 10L)).thenReturn(5L);

        StaffConflictException ex = assertThrows(StaffConflictException.class,
                () -> schoolStaffService.updateStaffStatus(1L, 10L, true, currentUser));

        assertTrue(ex.getMessage().contains("Maximum limit of 5 accompanying teachers reached"));
        verify(schoolStaffRepository, never()).save(any());
    }

    @Test
    void deleteStaff_Success() {
        when(schoolRepository.existsById(1L)).thenReturn(true);

        SchoolStaff staff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.ACCOMPANYING_TEACHER, true, currentUser);
        when(schoolStaffRepository.findByIdAndSchoolId(10L, 1L)).thenReturn(Optional.of(staff));

        SchoolStaff deletedStaff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.ACCOMPANYING_TEACHER, false, currentUser);
        when(schoolStaffRepository.save(any(SchoolStaff.class))).thenReturn(deletedStaff);

        schoolStaffService.deleteStaff(1L, 10L, currentUser);

        verify(schoolStaffRepository).save(any(SchoolStaff.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void deleteStaff_OnlyLoginTeacher_ThrowsStaffConflictException() {
        when(schoolRepository.existsById(1L)).thenReturn(true);

        SchoolStaff staff = SchoolStaffTestDataFactory.createStaffEntity(10L, activeSchool, StaffRole.LOGIN_TEACHER, true, currentUser);
        when(schoolStaffRepository.findByIdAndSchoolId(10L, 1L)).thenReturn(Optional.of(staff));
        when(schoolStaffRepository.countBySchoolIdAndStaffRoleAndActiveTrueAndIdNot(1L, StaffRole.LOGIN_TEACHER, 10L)).thenReturn(0L);

        StaffConflictException ex = assertThrows(StaffConflictException.class,
                () -> schoolStaffService.deleteStaff(1L, 10L, currentUser));

        assertTrue(ex.getMessage().contains("Cannot delete the only active LOGIN_TEACHER"));
        verify(schoolStaffRepository, never()).save(any());
    }
}
