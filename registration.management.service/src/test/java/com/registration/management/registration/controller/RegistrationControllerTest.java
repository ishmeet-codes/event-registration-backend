package com.registration.management.registration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.registration.management.common.exception.GlobalExceptionHandler;
import com.registration.management.common.exception.SchoolNotActiveException;
import com.registration.management.enums.RegistrationStatus;
import com.registration.management.registration.dto.*;
import com.registration.management.registration.exception.RegistrationConflictException;
import com.registration.management.registration.exception.RegistrationHasParticipantsException;
import com.registration.management.registration.exception.RegistrationNotFoundException;
import com.registration.management.registration.service.RegistrationService;
import com.registration.management.registration.util.RegistrationTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private RegistrationController registrationController;

    private ObjectMapper objectMapper;
    private RegistrationResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(registrationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = RegistrationTestDataFactory.createResponseDTO();
    }

    // 1. GET /api/registrations/{id}
    @Test
    void getRegistrationById_Success_Returns200() throws Exception {
        when(registrationService.getRegistrationById(eq(101L), any())).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/registrations/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.participantCount", is(8)))
                .andExpect(jsonPath("$.school.schoolName", is("ABC Public School")))
                .andExpect(jsonPath("$.event.eventName", is("APEX 2026")))
                .andExpect(jsonPath("$.createdByStaff.fullName", is("Rahul Sharma")));

        verify(registrationService, times(1)).getRegistrationById(eq(101L), any());
    }

    @Test
    void getRegistrationById_NotFound_Returns404() throws Exception {
        when(registrationService.getRegistrationById(eq(999L), any()))
                .thenThrow(new RegistrationNotFoundException("Registration not found with id: 999"));

        mockMvc.perform(get("/api/registrations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Registration not found with id: 999")));
    }

    // 2. GET /api/registrations
    @Test
    void getRegistrations_Success_Returns200WithPage() throws Exception {
        Page<RegistrationResponseDTO> page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 20), 1);
        when(registrationService.getRegistrations(
                eq("ABC"), any(), eq(5L), eq(10L), eq(15L),
                any(), any(), any(), any(), eq(0), eq(20), eq("createdAt,desc"), any()
        )).thenReturn(page);

        mockMvc.perform(get("/api/registrations")
                        .param("search", "ABC")
                        .param("status", "PENDING")
                        .param("schoolId", "5")
                        .param("eventId", "10")
                        .param("createdByStaffId", "15")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(101)))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    // 3. POST /api/registrations
    @Test
    void createRegistration_Success_Returns201() throws Exception {
        RegistrationCreateRequestDTO request = RegistrationTestDataFactory.createValidCreateRequestDTO();

        when(registrationService.createRegistration(any(RegistrationCreateRequestDTO.class), any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(101)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.school.id", is(5)))
                .andExpect(jsonPath("$.event.id", is(10)));
    }

    @Test
    void createRegistration_ValidationFailure_MissingRequiredFields_Returns400() throws Exception {
        RegistrationCreateRequestDTO invalidRequest = RegistrationCreateRequestDTO.builder().build();

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.schoolId", notNullValue()))
                .andExpect(jsonPath("$.errors.participants", notNullValue()));
    }

    @Test
    void createRegistration_Conflict_Returns409() throws Exception {
        RegistrationCreateRequestDTO request = RegistrationTestDataFactory.createValidCreateRequestDTO();

        when(registrationService.createRegistration(any(RegistrationCreateRequestDTO.class), any()))
                .thenThrow(new RegistrationConflictException("Registration already exists for school: 5 and event: 10"));

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Registration already exists for school: 5 and event: 10")));
    }

    @Test
    void createRegistration_SchoolNotActive_Returns400() throws Exception {
        RegistrationCreateRequestDTO request = RegistrationTestDataFactory.createValidCreateRequestDTO();

        when(registrationService.createRegistration(any(RegistrationCreateRequestDTO.class), any()))
                .thenThrow(new SchoolNotActiveException("School is not active with id: 5"));

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("School is not active with id: 5")));
    }

    // 4. PUT /api/registrations/{id}
    @Test
    void updateRegistration_Success_Returns200() throws Exception {
        RegistrationUpdateRequestDTO request = RegistrationTestDataFactory.createValidUpdateRequestDTO();

        when(registrationService.updateRegistration(eq(101L), any(RegistrationUpdateRequestDTO.class), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/registrations/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)));
    }

    @Test
    void updateRegistration_NotFound_Returns404() throws Exception {
        RegistrationUpdateRequestDTO request = RegistrationTestDataFactory.createValidUpdateRequestDTO();

        when(registrationService.updateRegistration(eq(999L), any(RegistrationUpdateRequestDTO.class), any()))
                .thenThrow(new RegistrationNotFoundException("Registration not found with id: 999"));

        mockMvc.perform(put("/api/registrations/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Registration not found with id: 999")));
    }

    // 5. DELETE /api/registrations/{id}
    @Test
    void deleteRegistration_Success_Returns204() throws Exception {
        doNothing().when(registrationService).deleteRegistration(eq(101L), any());

        mockMvc.perform(delete("/api/registrations/101"))
                .andExpect(status().isNoContent());

        verify(registrationService, times(1)).deleteRegistration(eq(101L), any());
    }

    @Test
    void deleteRegistration_HasParticipants_Returns409() throws Exception {
        doThrow(new RegistrationHasParticipantsException("Cannot delete registration with id: 101 because it has participants"))
                .when(registrationService).deleteRegistration(eq(101L), any());

        mockMvc.perform(delete("/api/registrations/101"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Cannot delete registration with id: 101 because it has participants")));
    }

    @Test
    void deleteRegistration_NotFound_Returns404() throws Exception {
        doThrow(new RegistrationNotFoundException("Registration not found with id: 999"))
                .when(registrationService).deleteRegistration(eq(999L), any());

        mockMvc.perform(delete("/api/registrations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Registration not found with id: 999")));
    }

    // 6. PATCH /api/registrations/{id}/status
    @Test
    void updateRegistrationStatus_Success_Returns200() throws Exception {
        RegistrationStatusUpdateRequestDTO request = RegistrationTestDataFactory.createValidStatusUpdateRequestDTO(RegistrationStatus.APPROVED);

        when(registrationService.updateRegistrationStatus(eq(101L), any(RegistrationStatusUpdateRequestDTO.class), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/registrations/101/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)));
    }

    @Test
    void updateRegistrationStatus_ValidationFailure_NullStatus_Returns400() throws Exception {
        RegistrationStatusUpdateRequestDTO invalidRequest = RegistrationStatusUpdateRequestDTO.builder().build();

        mockMvc.perform(patch("/api/registrations/101/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status", notNullValue()));
    }

    // 7. POST /api/registrations/{id}/submit
    @Test
    void submitRegistration_WithoutRemarks_Returns200() throws Exception {
        when(registrationService.submitRegistration(eq(101L), any(), any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/registrations/101/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)));
    }

    @Test
    void submitRegistration_WithRemarks_Returns200() throws Exception {
        RegistrationRemarksRequestDTO remarks = RegistrationRemarksRequestDTO.builder().remarks("Submitting now").build();
        when(registrationService.submitRegistration(eq(101L), any(RegistrationRemarksRequestDTO.class), any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/registrations/101/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remarks)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)));
    }

    // 8. PATCH /api/registrations/{id}/approve
    @Test
    void approveRegistration_Success_Returns200() throws Exception {
        when(registrationService.approveRegistration(eq(101L), any())).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/registrations/101/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)));
    }

    @Test
    void approveRegistration_InvalidStateTransition_Returns400() throws Exception {
        when(registrationService.approveRegistration(eq(101L), any()))
                .thenThrow(new IllegalArgumentException("Cannot approve registration in status: DRAFT"));

        mockMvc.perform(patch("/api/registrations/101/approve"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Cannot approve registration in status: DRAFT")));
    }

    // 9. PATCH /api/registrations/{id}/reject
    @Test
    void rejectRegistration_WithoutRemarks_Returns200() throws Exception {
        when(registrationService.rejectRegistration(eq(101L), any(), any())).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/registrations/101/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)));
    }

    @Test
    void rejectRegistration_WithRemarks_Returns200() throws Exception {
        RegistrationRemarksRequestDTO remarks = RegistrationRemarksRequestDTO.builder().remarks("Incomplete participant info").build();
        when(registrationService.rejectRegistration(eq(101L), any(RegistrationRemarksRequestDTO.class), any())).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/registrations/101/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remarks)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)));
    }

    // 10. PATCH /api/registrations/{id}/cancel
    @Test
    void cancelRegistration_WithoutRemarks_Returns200() throws Exception {
        when(registrationService.cancelRegistration(eq(101L), any(), any())).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/registrations/101/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)));
    }

    @Test
    void cancelRegistration_WithRemarks_Returns200() throws Exception {
        RegistrationRemarksRequestDTO remarks = RegistrationRemarksRequestDTO.builder().remarks("School withdrawn").build();
        when(registrationService.cancelRegistration(eq(101L), any(RegistrationRemarksRequestDTO.class), any())).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/registrations/101/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remarks)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)));
    }

    // 11. GET /api/registrations/statistics
    @Test
    void getRegistrationStatistics_Success_Returns200() throws Exception {
        RegistrationStatisticsDTO stats = RegistrationStatisticsDTO.builder()
                .total(250)
                .draft(10)
                .pending(45)
                .approved(150)
                .rejected(20)
                .cancelled(15)
                .completed(10)
                .build();

        when(registrationService.getRegistrationStatistics(eq(10L), eq(5L), any())).thenReturn(stats);

        mockMvc.perform(get("/api/registrations/statistics")
                        .param("eventId", "10")
                        .param("schoolId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(250)))
                .andExpect(jsonPath("$.draft", is(10)))
                .andExpect(jsonPath("$.pending", is(45)))
                .andExpect(jsonPath("$.approved", is(150)))
                .andExpect(jsonPath("$.rejected", is(20)))
                .andExpect(jsonPath("$.cancelled", is(15)))
                .andExpect(jsonPath("$.completed", is(10)));
    }

    // 12. GET /api/events/{eventId}/registrations
    @Test
    void getRegistrationsForEvent_Success_Returns200() throws Exception {
        Page<RegistrationResponseDTO> page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 20), 1);
        when(registrationService.getRegistrationsForEvent(eq(10L), eq("test"), any(), eq(0), eq(20), eq("createdAt,desc"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/events/10/registrations")
                        .param("search", "test")
                        .param("status", "APPROVED")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(101)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    // 13. GET /api/schools/{schoolId}/registrations
    @Test
    void getRegistrationsForSchool_Success_Returns200() throws Exception {
        Page<RegistrationResponseDTO> page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 20), 1);
        when(registrationService.getRegistrationsForSchool(eq(5L), eq("test"), any(), eq(10L), eq(0), eq(20), eq("createdAt,desc"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/schools/5/registrations")
                        .param("search", "test")
                        .param("status", "PENDING")
                        .param("eventId", "10")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(101)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }
}
