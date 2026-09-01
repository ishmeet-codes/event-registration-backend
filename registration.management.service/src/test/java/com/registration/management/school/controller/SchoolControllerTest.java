package com.registration.management.school.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.registration.management.common.exception.GlobalExceptionHandler;
import com.registration.management.school.dto.schoolDTO;
import com.registration.management.school.exception.SchoolCodeException;
import com.registration.management.school.service.schoolService;
import com.registration.management.school.util.SchoolTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SchoolControllerTest {

    private MockMvc mockMvc;

    @Mock
    private schoolService schoolService;

    @InjectMocks
    private schoolController schoolController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(schoolController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createSchool_Success() throws Exception {
        schoolDTO requestDto = SchoolTestDataFactory.createValidSchoolDTO();
        schoolDTO responseDto = SchoolTestDataFactory.createResponseSchoolDTO();

        when(schoolService.createSchool(any(schoolDTO.class), any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.schoolCode", is("SCH001")))
                .andExpect(jsonPath("$.schoolName", is("ABC Public School")))
                .andExpect(jsonPath("$.principalName", is("Rajesh Kumar")))
                .andExpect(jsonPath("$.board", is("CBSE")))
                .andExpect(jsonPath("$.address", is("Model Town")))
                .andExpect(jsonPath("$.city", is("Ludhiana")))
                .andExpect(jsonPath("$.district", is("Ludhiana")))
                .andExpect(jsonPath("$.state", is("Punjab")))
                .andExpect(jsonPath("$.pincode", is("141001")))
                .andExpect(jsonPath("$.phone", is("9876543210")))
                .andExpect(jsonPath("$.email", is("abcschool@example.com")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.createdById", is(10)))
                .andExpect(jsonPath("$.createdByName", is("Test Admin")));
    }

    @Test
    void createSchool_DuplicateSchoolCode_ReturnsBadRequest() throws Exception {
        schoolDTO requestDto = SchoolTestDataFactory.createValidSchoolDTO();

        when(schoolService.createSchool(any(schoolDTO.class), any()))
                .thenThrow(new SchoolCodeException("School code already exists: SCH001"));

        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("School code already exists: SCH001")));
    }

    @Test
    void createSchool_ValidationFailure_BlankRequiredFields() throws Exception {
        schoolDTO invalidDto = schoolDTO.builder()
                .schoolCode("")
                .schoolName("")
                .pincode("123")
                .phone("123")
                .email("invalid-email")
                .build();

        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.schoolCode", is("School code is required")))
                .andExpect(jsonPath("$.errors.schoolName", is("School name is required")))
                .andExpect(jsonPath("$.errors.pincode", is("Pincode must be 6 digits")))
                .andExpect(jsonPath("$.errors.phone", is("Phone number must be between 10 and 15 digits")))
                .andExpect(jsonPath("$.errors.email", is("Invalid email format")));
    }
}
