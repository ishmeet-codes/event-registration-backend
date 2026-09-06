package com.registration.management.checkin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.registration.management.checkin.dto.*;
import com.registration.management.checkin.enums.CheckinStatus;
import com.registration.management.checkin.service.CheckinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CheckinControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CheckinService checkinService;

    @InjectMocks
    private CheckinController checkinController;

    private ObjectMapper objectMapper;
    private CheckinResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(checkinController)
                .setControllerAdvice(new com.registration.management.common.exception.GlobalExceptionHandler())
                .build();

        responseDTO = CheckinResponseDTO.builder()
                .id(501L)
                .participant(CheckinResponseDTO.ParticipantSummary.builder().id(101L).name("Rahul").className("11").build())
                .school(CheckinResponseDTO.SchoolSummary.builder().id(10L).name("NSPS").code("SCH001").build())
                .event(CheckinResponseDTO.EventSummary.builder().id(5L).name("SQL Masters").build())
                .registration(CheckinResponseDTO.RegistrationSummary.builder().id(100L).build())
                .status(CheckinStatus.CHECKED_IN)
                .checkedInAt(LocalDateTime.now())
                .remarks("Verified")
                .build();
    }

    @Test
    @DisplayName("POST /api/checkins - 201 Created")
    void checkinParticipant_Success() throws Exception {
        CheckinRequestDTO request = CheckinRequestDTO.builder()
                .participantId(101L)
                .remarks("Verified")
                .build();

        when(checkinService.checkinParticipant(any(CheckinRequestDTO.class), any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/checkins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(501L))
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));
    }

    @Test
    @DisplayName("GET /api/checkins/{id} - 200 OK")
    void getCheckinById_Success() throws Exception {
        when(checkinService.getCheckinById(501L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/checkins/501"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(501L));
    }

    @Test
    @DisplayName("DELETE /api/checkins/{id} - 244 No Content")
    void deleteCheckin_Success() throws Exception {
        doNothing().when(checkinService).deleteCheckin(eq(501L), any());

        mockMvc.perform(delete("/api/checkins/501"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/checkins - 200 OK")
    void getCheckins_Success() throws Exception {
        when(checkinService.getCheckins(
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(Long.class),
                org.mockito.ArgumentMatchers.nullable(Long.class),
                org.mockito.ArgumentMatchers.nullable(Long.class),
                org.mockito.ArgumentMatchers.nullable(CheckinStatus.class),
                org.mockito.ArgumentMatchers.nullable(Long.class),
                org.mockito.ArgumentMatchers.nullable(java.time.LocalDate.class),
                org.mockito.ArgumentMatchers.nullable(java.time.LocalDateTime.class),
                org.mockito.ArgumentMatchers.nullable(java.time.LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(responseDTO), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/checkins"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(501L));
    }

    @Test
    @DisplayName("POST /api/checkins/{id}/checkout - 200 OK")
    void checkoutParticipant_Success() throws Exception {
        responseDTO.setStatus(CheckinStatus.CHECKED_OUT);
        when(checkinService.checkoutParticipant(eq(501L), any(), any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/checkins/501/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));
    }

    @Test
    @DisplayName("GET /api/checkins/event/{eventId}/summary - 200 OK")
    void getEventSummary_Success() throws Exception {
        EventCheckinSummaryDTO summaryDTO = EventCheckinSummaryDTO.builder()
                .eventId(5L)
                .eventName("SQL Masters")
                .totalParticipants(100)
                .checkedIn(72)
                .checkedOut(40)
                .notCheckedIn(20)
                .absent(8)
                .attendancePercentage(72.0)
                .build();

        when(checkinService.getEventSummary(5L)).thenReturn(summaryDTO);

        mockMvc.perform(get("/api/checkins/event/5/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(5L))
                .andExpect(jsonPath("$.checkedIn").value(72));
    }
}
