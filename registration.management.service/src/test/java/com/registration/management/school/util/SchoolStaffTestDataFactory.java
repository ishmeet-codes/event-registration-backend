package com.registration.management.school.util;

import com.registration.management.auth.entities.User;
import com.registration.management.enums.StaffRole;
import com.registration.management.school.dto.schoolStaffDTO;
import com.registration.management.school.entity.School;
import com.registration.management.school.entity.SchoolStaff;

public class SchoolStaffTestDataFactory {

    public static schoolStaffDTO createValidStaffDTO(StaffRole role) {
        return schoolStaffDTO.builder()
                .schoolId(1L)
                .fullName("Aman Sharma")
                .designation("Teacher")
                .phone("9876543210")
                .email("aman@example.com")
                .staffRole(role != null ? role : StaffRole.ACCOMPANYING_TEACHER)
                .build();
    }

    public static schoolStaffDTO createResponseStaffDTO(Long id, Long schoolId, StaffRole role) {
        return schoolStaffDTO.builder()
                .id(id != null ? id : 12L)
                .schoolId(schoolId != null ? schoolId : 1L)
                .fullName("Aman Sharma")
                .designation("Teacher")
                .phone("9876543210")
                .email("aman@example.com")
                .staffRole(role != null ? role : StaffRole.ACCOMPANYING_TEACHER)
                .active(true)
                .createdById(10L)
                .createdByName("Test Admin")
                .build();
    }

    public static SchoolStaff createStaffEntity(Long id, School school, StaffRole role, boolean active, User user) {
        return SchoolStaff.builder()
                .id(id)
                .school(school)
                .fullName("Aman Sharma")
                .designation("Teacher")
                .phone("9876543210")
                .email("aman@example.com")
                .staffRole(role != null ? role : StaffRole.ACCOMPANYING_TEACHER)
                .active(active)
                .createdBy(user)
                .build();
    }
}
