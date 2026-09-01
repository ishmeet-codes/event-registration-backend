package com.registration.management.school.util;

import com.registration.management.auth.entities.User;
import com.registration.management.school.dto.schoolDTO;
import com.registration.management.school.entity.School;

public class SchoolTestDataFactory {

    public static schoolDTO createValidSchoolDTO() {
        return schoolDTO.builder()
                .schoolCode("SCH001")
                .schoolName("ABC Public School")
                .principalName("Rajesh Kumar")
                .board("CBSE")
                .address("Model Town")
                .city("Ludhiana")
                .district("Ludhiana")
                .state("Punjab")
                .pincode("141001")
                .phone("9876543210")
                .email("abcschool@example.com")
                .build();
    }

    public static schoolDTO createResponseSchoolDTO() {
        return schoolDTO.builder()
                .id(1L)
                .schoolCode("SCH001")
                .schoolName("ABC Public School")
                .principalName("Rajesh Kumar")
                .board("CBSE")
                .address("Model Town")
                .city("Ludhiana")
                .district("Ludhiana")
                .state("Punjab")
                .pincode("141001")
                .phone("9876543210")
                .email("abcschool@example.com")
                .active(true)
                .createdById(10L)
                .createdByName("Test Admin")
                .build();
    }

    public static School createSchoolEntity(User user) {
        return School.builder()
                .id(100L)
                .schoolCode("SCH001")
                .schoolName("ABC Public School")
                .principalName("Rajesh Kumar")
                .board("CBSE")
                .address("Model Town")
                .city("Ludhiana")
                .district("Ludhiana")
                .state("Punjab")
                .pincode("141001")
                .phone("9876543210")
                .email("abcschool@example.com")
                .active(true)
                .createdBy(user)
                .build();
    }

    public static User createTestUser() {
        return User.builder()
                .id(10L)
                .fullName("Test Admin")
                .email("admin@registration.com")
                .active(true)
                .build();
    }
}
