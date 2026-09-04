package com.registration.management.school.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.registration.management.common.exception.GlobalExceptionHandler;
import com.registration.management.school.dto.SchoolStatusRequestDTO;
import com.registration.management.school.dto.schoolDTO;
import com.registration.management.common.exception.SchoolCodeException;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void createSchool_WithoutSchoolCode_Success() throws Exception {
        schoolDTO requestDto = SchoolTestDataFactory.createValidSchoolDTO();
        requestDto.setSchoolCode(null);
        schoolDTO responseDto = SchoolTestDataFactory.createResponseSchoolDTO();
        responseDto.setSchoolCode("SCHA1B2C3");

        when(schoolService.createSchool(any(schoolDTO.class), any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.schoolCode", is("SCHA1B2C3")));
    }

    @Test
    void createSchool_ValidationFailure_BlankRequiredFields() throws Exception {
        schoolDTO invalidDto = schoolDTO.builder()
                .schoolName("")
                .pincode("123")
                .phone("123")
                .email("invalid-email")
                .build();

        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.schoolName", is("School name is required")))
                .andExpect(jsonPath("$.errors.pincode", is("Pincode must be 6 digits")))
                .andExpect(jsonPath("$.errors.phone", is("Phone number must be between 10 and 15 digits")))
                .andExpect(jsonPath("$.errors.email", is("Invalid email format")));
    }

    @Test
    void getSchools_Success() throws Exception {
        schoolDTO school = SchoolTestDataFactory.createResponseSchoolDTO();
        Page<schoolDTO> page = new PageImpl<>(List.of(school), PageRequest.of(0, 20), 1);

        when(schoolService.getSchools(
                eq("public"), eq("Ludhiana"), eq("Ludhiana"), eq("Punjab"), eq("CBSE"), eq(true), eq(0), eq(20), eq("schoolName,asc")))
                .thenReturn(page);

        mockMvc.perform(get("/api/schools")
                        .param("search", "public")
                        .param("city", "Ludhiana")
                        .param("district", "Ludhiana")
                        .param("state", "Punjab")
                        .param("board", "CBSE")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "schoolName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].schoolCode", is("SCH001")))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void getSchoolById_Success_WithoutSummary() throws Exception {
        schoolDTO school = SchoolTestDataFactory.createResponseSchoolDTO();

        when(schoolService.getSchoolById(1L, false)).thenReturn(school);

        mockMvc.perform(get("/api/schools/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.schoolCode", is("SCH001")))
                .andExpect(jsonPath("$.schoolName", is("ABC Public School")))
                .andExpect(jsonPath("$.principalName", is("Rajesh Kumar")))
                .andExpect(jsonPath("$.board", is("CBSE")))
                .andExpect(jsonPath("$.city", is("Ludhiana")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.staffCount").doesNotExist())
                .andExpect(jsonPath("$.registrationCount").doesNotExist());
    }

    @Test
    void getSchoolById_Success_WithIncludeSummary() throws Exception {
        schoolDTO school = SchoolTestDataFactory.createResponseSchoolDTO();
        school.setStaffCount(4L);
        school.setRegistrationCount(3L);

        when(schoolService.getSchoolById(1L, true)).thenReturn(school);

        mockMvc.perform(get("/api/schools/1")
                        .param("includeSummary", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.schoolCode", is("SCH001")))
                .andExpect(jsonPath("$.schoolName", is("ABC Public School")))
                .andExpect(jsonPath("$.principalName", is("Rajesh Kumar")))
                .andExpect(jsonPath("$.board", is("CBSE")))
                .andExpect(jsonPath("$.city", is("Ludhiana")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.staffCount", is(4)))
                .andExpect(jsonPath("$.registrationCount", is(3)));
    }

    @Test
    void getSchoolById_Success_WithSummaryParam() throws Exception {
        schoolDTO school = SchoolTestDataFactory.createResponseSchoolDTO();
        school.setStaffCount(4L);
        school.setRegistrationCount(3L);

        when(schoolService.getSchoolById(1L, true)).thenReturn(school);

        mockMvc.perform(get("/api/schools/1")
                        .param("summary", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.staffCount", is(4)))
                .andExpect(jsonPath("$.registrationCount", is(3)));
    }

    @Test
    void getSchoolById_NotFound_Returns404() throws Exception {
        when(schoolService.getSchoolById(999L, false))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("School not found with id: 999"));

        mockMvc.perform(get("/api/schools/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("School not found with id: 999")));
    }

    @Test
    void updateSchool_Success() throws Exception {
        schoolDTO requestDto = SchoolTestDataFactory.createValidSchoolDTO();
        schoolDTO responseDto = SchoolTestDataFactory.createResponseSchoolDTO();
        responseDto.setSchoolName("Updated School Name");

        when(schoolService.updateSchool(eq(1L), any(schoolDTO.class), any())).thenReturn(responseDto);

        mockMvc.perform(put("/api/schools/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.schoolName", is("Updated School Name")));
    }

    @Test
    void updateSchool_DuplicateSchoolCode_ReturnsBadRequest() throws Exception {
        schoolDTO requestDto = SchoolTestDataFactory.createValidSchoolDTO();

        when(schoolService.updateSchool(eq(1L), any(schoolDTO.class), any()))
                .thenThrow(new SchoolCodeException("School code already exists: SCH001"));

        mockMvc.perform(put("/api/schools/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("School code already exists: SCH001")));
    }

    @Test
    void updateSchool_NotFound_Returns404() throws Exception {
        schoolDTO requestDto = SchoolTestDataFactory.createValidSchoolDTO();

        when(schoolService.updateSchool(eq(999L), any(schoolDTO.class), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("School not found with id: 999"));

        mockMvc.perform(put("/api/schools/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("School not found with id: 999")));
    }

    @Test
    void updateSchoolStatus_Success() throws Exception {
        SchoolStatusRequestDTO statusRequest = new SchoolStatusRequestDTO(false);
        schoolDTO responseDto = SchoolTestDataFactory.createResponseSchoolDTO();
        responseDto.setActive(false);

        when(schoolService.updateSchoolStatus(eq(1L), eq(false), any())).thenReturn(responseDto);

        mockMvc.perform(patch("/api/schools/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void updateSchoolStatus_ValidationFailure_NullActive() throws Exception {
        mockMvc.perform(patch("/api/schools/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.active", is("Active status is required")));
    }

    @Test
    void updateSchoolStatus_NotFound_Returns404() throws Exception {
        SchoolStatusRequestDTO statusRequest = new SchoolStatusRequestDTO(true);

        when(schoolService.updateSchoolStatus(eq(999L), eq(true), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("School not found with id: 999"));

        mockMvc.perform(patch("/api/schools/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("School not found with id: 999")));
    }

    @Test
    void deleteSchool_Success() throws Exception {
        doNothing().when(schoolService).deleteSchool(eq(1L), any());

        mockMvc.perform(delete("/api/schools/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSchool_NotFound_Returns404() throws Exception {
        doThrow(new jakarta.persistence.EntityNotFoundException("School not found with id: 999"))
                .when(schoolService).deleteSchool(eq(999L), any());

        mockMvc.perform(delete("/api/schools/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("School not found with id: 999")));
    }
}
