package com.registration.management.school.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.registration.management.common.exception.GlobalExceptionHandler;
import com.registration.management.enums.StaffRole;
import com.registration.management.school.dto.StaffStatusRequestDTO;
import com.registration.management.school.dto.schoolStaffDTO;
import com.registration.management.school.exception.StaffConflictException;
import com.registration.management.school.service.schoolStaffService;
import com.registration.management.school.util.SchoolStaffTestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SchoolStaffControllerTest {

    private MockMvc mockMvc;

    @Mock
    private schoolStaffService schoolStaffService;

    @InjectMocks
    private schoolStaffController schoolStaffController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(schoolStaffController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createStaff_Success_Returns201() throws Exception {
        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.ACCOMPANYING_TEACHER);
        schoolStaffDTO response = SchoolStaffTestDataFactory.createResponseStaffDTO(12L, 1L, StaffRole.ACCOMPANYING_TEACHER);

        when(schoolStaffService.createStaff(eq(1L), any(schoolStaffDTO.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/schools/1/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(12)))
                .andExpect(jsonPath("$.schoolId", is(1)))
                .andExpect(jsonPath("$.fullName", is("Aman Sharma")))
                .andExpect(jsonPath("$.designation", is("Teacher")))
                .andExpect(jsonPath("$.phone", is("9876543210")))
                .andExpect(jsonPath("$.email", is("aman@example.com")))
                .andExpect(jsonPath("$.staffRole", is("ACCOMPANYING_TEACHER")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void createStaff_Conflict_Returns409() throws Exception {
        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.LOGIN_TEACHER);

        when(schoolStaffService.createStaff(eq(1L), any(schoolStaffDTO.class), any()))
                .thenThrow(new StaffConflictException("A LOGIN_TEACHER already exists for school: 1"));

        mockMvc.perform(post("/api/schools/1/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("A LOGIN_TEACHER already exists for school: 1")));
    }

    @Test
    void createStaff_InvalidEmail_Returns400() throws Exception {
        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.ACCOMPANYING_TEACHER);
        request.setEmail("invalid-email");

        mockMvc.perform(post("/api/schools/1/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email", is("Invalid email format")));
    }

    @Test
    void getStaffList_Success_Returns200() throws Exception {
        schoolStaffDTO staffDto = SchoolStaffTestDataFactory.createResponseStaffDTO(12L, 1L, StaffRole.ACCOMPANYING_TEACHER);
        Page<schoolStaffDTO> page = new PageImpl<>(List.of(staffDto), org.springframework.data.domain.PageRequest.of(0, 20), 1);

        when(schoolStaffService.getStaffList(eq(1L), any(), any(), any(), eq(0), eq(20), eq("fullName,asc")))
                .thenReturn(page);

        mockMvc.perform(get("/api/schools/1/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(12)))
                .andExpect(jsonPath("$.content[0].fullName", is("Aman Sharma")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void getStaffById_Success_Returns200() throws Exception {
        schoolStaffDTO staffDto = SchoolStaffTestDataFactory.createResponseStaffDTO(12L, 1L, StaffRole.ACCOMPANYING_TEACHER);

        when(schoolStaffService.getStaffById(1L, 12L)).thenReturn(staffDto);

        mockMvc.perform(get("/api/schools/1/staff/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(12)))
                .andExpect(jsonPath("$.schoolId", is(1)))
                .andExpect(jsonPath("$.fullName", is("Aman Sharma")))
                .andExpect(jsonPath("$.staffRole", is("ACCOMPANYING_TEACHER")));
    }

    @Test
    void getStaffById_NotFound_Returns404() throws Exception {
        when(schoolStaffService.getStaffById(1L, 999L))
                .thenThrow(new EntityNotFoundException("Staff not found with id: 999 for school: 1"));

        mockMvc.perform(get("/api/schools/1/staff/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Staff not found with id: 999 for school: 1")));
    }

    @Test
    void updateStaff_Success_Returns200() throws Exception {
        schoolStaffDTO request = SchoolStaffTestDataFactory.createValidStaffDTO(StaffRole.ACCOMPANYING_TEACHER);
        schoolStaffDTO response = SchoolStaffTestDataFactory.createResponseStaffDTO(12L, 1L, StaffRole.ACCOMPANYING_TEACHER);

        when(schoolStaffService.updateStaff(eq(1L), eq(12L), any(schoolStaffDTO.class), any())).thenReturn(response);

        mockMvc.perform(put("/api/schools/1/staff/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(12)))
                .andExpect(jsonPath("$.fullName", is("Aman Sharma")));
    }

    @Test
    void updateStaffStatus_Success_Returns200() throws Exception {
        StaffStatusRequestDTO request = StaffStatusRequestDTO.builder().active(false).build();
        schoolStaffDTO response = SchoolStaffTestDataFactory.createResponseStaffDTO(12L, 1L, StaffRole.ACCOMPANYING_TEACHER);
        response.setActive(false);

        when(schoolStaffService.updateStaffStatus(eq(1L), eq(12L), eq(false), any())).thenReturn(response);

        mockMvc.perform(patch("/api/schools/1/staff/12/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void deleteStaff_Success_Returns204() throws Exception {
        doNothing().when(schoolStaffService).deleteStaff(eq(1L), eq(12L), any());

        mockMvc.perform(delete("/api/schools/1/staff/12"))
                .andExpect(status().isNoContent());
    }
}
