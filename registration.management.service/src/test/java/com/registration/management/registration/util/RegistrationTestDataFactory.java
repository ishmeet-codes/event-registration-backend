package com.registration.management.registration.util;

import com.registration.management.auth.entities.Role;
import com.registration.management.auth.entities.User;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.event.entities.Event;
import com.registration.management.registration.dto.*;
import com.registration.management.registration.entity.Registration;
import com.registration.management.school.entity.School;
import com.registration.management.school.entity.SchoolStaff;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RegistrationTestDataFactory {

    public static RegistrationCreateRequestDTO createValidCreateRequestDTO() {
        return RegistrationCreateRequestDTO.builder()
                .schoolId(5L)
                .eventId(10L)
                .createdByStaffId(15L)
                .remarks("Registration for APEX 2026")
                .build();
    }

    public static RegistrationUpdateRequestDTO createValidUpdateRequestDTO() {
        return RegistrationUpdateRequestDTO.builder()
                .remarks("Updated registration remarks")
                .build();
    }

    public static RegistrationStatusUpdateRequestDTO createValidStatusUpdateRequestDTO(RegistrationStatus status) {
        return RegistrationStatusUpdateRequestDTO.builder()
                .status(status)
                .remarks("Status updated")
                .build();
    }

    public static RegistrationResponseDTO createResponseDTO() {
        return RegistrationResponseDTO.builder()
                .id(101L)
                .school(SchoolSummaryDTO.builder().id(5L).schoolCode("SCH001").schoolName("ABC Public School").build())
                .event(EventSummaryDTO.builder().id(10L).eventName("APEX 2026").build())
                .createdByStaff(StaffSummaryDTO.builder().id(15L).fullName("Rahul Sharma").build())
                .status(RegistrationStatus.PENDING)
                .remarks("School registration")
                .participantCount(8L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static School createTestSchool() {
        return School.builder()
                .id(5L)
                .schoolCode("SCH001")
                .schoolName("ABC Public School")
                .active(true)
                .build();
    }

    public static Event createTestEvent() {
        return Event.builder()
                .id(10L)
                .eventName("APEX 2026")
                .active(true)
                .maxRegistrations(50)
                .registrationDeadline(LocalDateTime.now().plusDays(10))
                .eventDate(LocalDate.now().plusDays(15))
                .build();
    }

    public static SchoolStaff createTestStaff(School school) {
        return SchoolStaff.builder()
                .id(15L)
                .fullName("Rahul Sharma")
                .school(school)
                .active(true)
                .build();
    }

    public static User createTestUser() {
        Role adminRole = Role.builder().id(1L).roleCode("ADMIN").roleName("Admin").build();
        return User.builder()
                .id(1L)
                .fullName("Admin User")
                .email("admin@registration.com")
                .role(adminRole)
                .active(true)
                .build();
    }

    public static Registration createRegistrationEntity(School school, Event event, SchoolStaff staff, User user) {
        return Registration.builder()
                .id(101L)
                .school(school)
                .event(event)
                .createdByStaff(staff)
                .status(RegistrationStatus.DRAFT)
                .remarks("Initial registration")
                .createdBy(user)
                .updatedBy(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
