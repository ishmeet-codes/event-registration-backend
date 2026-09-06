package com.registration.management.report.service;

import com.registration.management.auth.entities.Role;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.checkin.repository.CheckinRepository;
import com.registration.management.enums.Gender;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.event.repository.EventRepository;
import com.registration.management.participant.entity.Participant;
import com.registration.management.participant.repository.ParticipantRepository;
import com.registration.management.registration.entity.Registration;
import com.registration.management.registration.repository.RegistrationRepository;
import com.registration.management.report.dto.*;
import com.registration.management.report.serviceImpl.ReportServiceImpl;
import com.registration.management.school.entity.School;
import com.registration.management.school.repository.schoolRepository;
import com.registration.management.school.repository.schoolStaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private schoolRepository schoolRepository;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private schoolStaffRepository schoolStaffRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private User adminUser;
    private User teacherUser;
    private School school1;
    private Registration registration1;
    private Participant participant1;

    @BeforeEach
    void setUp() {
        Role adminRole = Role.builder().id(1L).roleCode("ADMIN").roleName("Admin").build();
        adminUser = User.builder().id(1L).email("admin@test.com").role(adminRole).build();

        Role teacherRole = Role.builder().id(2L).roleCode("LOGIN_TEACHER").roleName("Teacher").build();
        teacherUser = User.builder().id(2L).email("teacher@test.com").role(teacherRole).build();

        school1 = School.builder().id(100L).schoolCode("SCH100").schoolName("Greenwood High").city("Metropolis").build();

        registration1 = Registration.builder()
                .id(1L)
                .school(school1)
                .status(RegistrationStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build();

        participant1 = Participant.builder()
                .id(1L)
                .fullName("Alice Smith")
                .gender(Gender.FEMALE)
                .className("10th")
                .registration(registration1)
                .build();
    }

    @Test
    @DisplayName("Dashboard report as Admin returns overall statistics")
    void getDashboardReport_AsAdmin_ReturnsOverallStats() {
        when(registrationRepository.findAll()).thenReturn(List.of(registration1));
        when(participantRepository.findAll()).thenReturn(List.of(participant1));
        when(checkinRepository.findAll()).thenReturn(Collections.emptyList());
        when(schoolRepository.count()).thenReturn(2L);
        when(eventRepository.count()).thenReturn(1L);

        DashboardReportDTO dto = reportService.getDashboardReport(adminUser);

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalRegistrations()).isEqualTo(1);
        assertThat(dto.getTotalParticipants()).isEqualTo(1);
        assertThat(dto.getTotalSchools()).isEqualTo(2);
        assertThat(dto.getTotalEvents()).isEqualTo(1);
    }

    @Test
    @DisplayName("Teacher viewing report for unauthorized school throws AccessDeniedException")
    void getSchoolReportById_UnauthorizedSchool_ThrowsAccessDenied() {
        when(schoolStaffRepository.findSchoolIdsByUserId(2L)).thenReturn(List.of(100L));

        assertThatThrownBy(() -> reportService.getSchoolReportById(200L, teacherUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You can only view reports for your assigned school");
    }

    @Test
    @DisplayName("Teacher viewing report for authorized school returns school details")
    void getSchoolReportById_AuthorizedSchool_ReturnsDetails() {
        when(schoolStaffRepository.findSchoolIdsByUserId(2L)).thenReturn(List.of(100L));
        when(schoolRepository.findById(100L)).thenReturn(Optional.of(school1));
        when(registrationRepository.findAll()).thenReturn(List.of(registration1));
        when(participantRepository.findAll()).thenReturn(List.of(participant1));

        IndividualSchoolReportDTO dto = reportService.getSchoolReportById(100L, teacherUser);

        assertThat(dto).isNotNull();
        assertThat(dto.getSchoolId()).isEqualTo(100L);
        assertThat(dto.getSchoolName()).isEqualTo("Greenwood High");
    }

    @Test
    @DisplayName("Export CSV report returns valid CSV byte array")
    void exportReport_ReturnsCsvByteArray() {
        when(registrationRepository.findAll()).thenReturn(List.of(registration1));

        byte[] csvBytes = reportService.exportReport("REGISTRATION", "csv", null, null, adminUser);

        assertThat(csvBytes).isNotNull();
        String csvContent = new String(csvBytes);
        assertThat(csvContent).contains("ID,Registration Number,School Name");
        assertThat(csvContent).contains("REG-00001");
    }
}
