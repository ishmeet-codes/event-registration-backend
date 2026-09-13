package com.registration.management.refreshment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.registration.management.refreshment.dto.*;
import com.registration.management.refreshment.enums.DistributionStatus;
import com.registration.management.refreshment.enums.RecipientCategory;
import com.registration.management.refreshment.enums.TrackingType;
import com.registration.management.refreshment.service.RefreshmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RefreshmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RefreshmentService refreshmentService;

    @InjectMocks
    private RefreshmentController refreshmentController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(refreshmentController)
                .setControllerAdvice(new com.registration.management.common.exception.GlobalExceptionHandler())
                .build();
    }

    // =========================================================================
    // 1. PLANS
    // =========================================================================

    @Test
    @DisplayName("POST /api/refreshment/plans - 201 Created")
    void createPlan_Success() throws Exception {
        RefreshmentPlanRequestDTO request = RefreshmentPlanRequestDTO.builder()
                .name("APEX 2026 Refreshment Plan")
                .eventDate(LocalDate.of(2026, 10, 15))
                .description("Master plan for refreshments")
                .active(true)
                .build();

        RefreshmentPlanResponseDTO response = RefreshmentPlanResponseDTO.builder()
                .id(1L)
                .name("APEX 2026 Refreshment Plan")
                .eventDate(LocalDate.of(2026, 10, 15))
                .active(true)
                .totalSessions(0)
                .build();

        when(refreshmentService.createPlan(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/refreshment/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("APEX 2026 Refreshment Plan"));
    }

    @Test
    @DisplayName("GET /api/refreshment/plans - 200 OK")
    void getAllPlans_Success() throws Exception {
        RefreshmentPlanResponseDTO plan = RefreshmentPlanResponseDTO.builder()
                .id(1L)
                .name("APEX 2026")
                .eventDate(LocalDate.of(2026, 10, 15))
                .active(true)
                .build();

        when(refreshmentService.getAllPlans()).thenReturn(List.of(plan));

        mockMvc.perform(get("/api/refreshment/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("APEX 2026"));
    }

    @Test
    @DisplayName("GET /api/refreshment/plans/{id} - 200 OK")
    void getPlanById_Success() throws Exception {
        RefreshmentPlanResponseDTO plan = RefreshmentPlanResponseDTO.builder()
                .id(1L)
                .name("APEX 2026")
                .eventDate(LocalDate.of(2026, 10, 15))
                .active(true)
                .build();

        when(refreshmentService.getPlanById(1L)).thenReturn(plan);

        mockMvc.perform(get("/api/refreshment/plans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/refreshment/plans/{id} - 200 OK")
    void updatePlan_Success() throws Exception {
        RefreshmentPlanRequestDTO request = RefreshmentPlanRequestDTO.builder()
                .name("Updated Plan")
                .eventDate(LocalDate.of(2026, 10, 16))
                .build();

        RefreshmentPlanResponseDTO response = RefreshmentPlanResponseDTO.builder()
                .id(1L)
                .name("Updated Plan")
                .eventDate(LocalDate.of(2026, 10, 16))
                .build();

        when(refreshmentService.updatePlan(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/refreshment/plans/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Plan"));
    }

    @Test
    @DisplayName("DELETE /api/refreshment/plans/{id} - 204 No Content")
    void deletePlan_Success() throws Exception {
        doNothing().when(refreshmentService).deletePlan(eq(1L), any());

        mockMvc.perform(delete("/api/refreshment/plans/1"))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // 2. SESSIONS
    // =========================================================================

    @Test
    @DisplayName("POST /api/refreshment/sessions - 201 Created")
    void createSession_Success() throws Exception {
        RefreshmentSessionRequestDTO request = RefreshmentSessionRequestDTO.builder()
                .planId(1L)
                .name("SQL Master Lunch")
                .eventId(10L)
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .trackingType(TrackingType.INDIVIDUAL)
                .displayOrder(1)
                .build();

        RefreshmentSessionResponseDTO response = RefreshmentSessionResponseDTO.builder()
                .id(101L)
                .planId(1L)
                .name("SQL Master Lunch")
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .trackingType(TrackingType.INDIVIDUAL)
                .eligibleCount(30L)
                .presentCount(26L)
                .distributedCount(24L)
                .pendingCount(2L)
                .build();

        when(refreshmentService.createSession(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/refreshment/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.name").value("SQL Master Lunch"));
    }

    @Test
    @DisplayName("GET /api/refreshment/sessions/{id} - 200 OK")
    void getSessionById_Success() throws Exception {
        RefreshmentSessionResponseDTO response = RefreshmentSessionResponseDTO.builder()
                .id(101L)
                .name("SQL Master Lunch")
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .build();

        when(refreshmentService.getSessionById(101L)).thenReturn(response);

        mockMvc.perform(get("/api/refreshment/sessions/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101));
    }

    @Test
    @DisplayName("GET /api/refreshment/plans/{planId}/sessions - 200 OK")
    void getSessionsByPlan_Success() throws Exception {
        RefreshmentSessionResponseDTO session = RefreshmentSessionResponseDTO.builder()
                .id(101L)
                .name("SQL Master Lunch")
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .build();

        when(refreshmentService.getSessionsByPlan(1L)).thenReturn(List.of(session));

        mockMvc.perform(get("/api/refreshment/plans/1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101));
    }

    @Test
    @DisplayName("PUT /api/refreshment/sessions/{id} - 200 OK")
    void updateSession_Success() throws Exception {
        RefreshmentSessionRequestDTO request = RefreshmentSessionRequestDTO.builder()
                .planId(1L)
                .name("Updated Session")
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .build();

        RefreshmentSessionResponseDTO response = RefreshmentSessionResponseDTO.builder()
                .id(101L)
                .name("Updated Session")
                .build();

        when(refreshmentService.updateSession(eq(101L), any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/refreshment/sessions/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Session"));
    }

    @Test
    @DisplayName("DELETE /api/refreshment/sessions/{id} - 204 No Content")
    void deleteSession_Success() throws Exception {
        doNothing().when(refreshmentService).deleteSession(eq(101L), any());

        mockMvc.perform(delete("/api/refreshment/sessions/101"))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // 3. TEAMS & ASSIGNMENTS
    // =========================================================================

    @Test
    @DisplayName("POST /api/refreshment/teams - 201 Created")
    void createTeam_Success() throws Exception {
        RefreshmentTeamRequestDTO request = RefreshmentTeamRequestDTO.builder()
                .name("Team A")
                .description("SQL Master distribution team")
                .leaderId(5L)
                .memberUserIds(List.of(5L, 6L))
                .build();

        RefreshmentTeamResponseDTO response = RefreshmentTeamResponseDTO.builder()
                .id(10L)
                .name("Team A")
                .leaderName("Rahul Leader")
                .build();

        when(refreshmentService.createTeam(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/refreshment/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Team A"));
    }

    @Test
    @DisplayName("GET /api/refreshment/teams - 200 OK")
    void getAllTeams_Success() throws Exception {
        RefreshmentTeamResponseDTO team = RefreshmentTeamResponseDTO.builder()
                .id(10L)
                .name("Team A")
                .build();

        when(refreshmentService.getAllTeams()).thenReturn(List.of(team));

        mockMvc.perform(get("/api/refreshment/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Team A"));
    }

    @Test
    @DisplayName("POST /api/refreshment/assignments - 201 Created")
    void assignTeamToSession_Success() throws Exception {
        RefreshmentAssignmentRequestDTO request = RefreshmentAssignmentRequestDTO.builder()
                .sessionId(101L)
                .teamId(10L)
                .build();

        doNothing().when(refreshmentService).assignTeamToSession(any(), any());

        mockMvc.perform(post("/api/refreshment/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /api/refreshment/assignments/{sessionId}/{teamId} - 204 No Content")
    void removeTeamAssignment_Success() throws Exception {
        doNothing().when(refreshmentService).removeTeamAssignment(eq(101L), eq(10L), any());

        mockMvc.perform(delete("/api/refreshment/assignments/101/10"))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // 4. TEAM PORTAL
    // =========================================================================

    @Test
    @DisplayName("GET /api/refreshment/team/my-sessions - 200 OK")
    void getMyAssignedSessions_Success() throws Exception {
        TeamSessionSummaryDTO summary = TeamSessionSummaryDTO.builder()
                .sessionId(101L)
                .sessionName("SQL Master Lunch")
                .eventName("SQL Master")
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .trackingType(TrackingType.INDIVIDUAL)
                .eligibleCount(30L)
                .presentCount(26L)
                .distributedCount(24L)
                .pendingCount(2L)
                .build();

        when(refreshmentService.getMyAssignedSessions(any())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/refreshment/team/my-sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(101))
                .andExpect(jsonPath("$[0].presentCount").value(26))
                .andExpect(jsonPath("$[0].distributedCount").value(24))
                .andExpect(jsonPath("$[0].pendingCount").value(2));
    }

    @Test
    @DisplayName("GET /api/refreshment/sessions/{sessionId}/recipients - 200 OK")
    void getSessionRecipients_Success() throws Exception {
        RecipientDistributionDTO recipient = RecipientDistributionDTO.builder()
                .recipientId(501L)
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .name("Aman Singh")
                .schoolName("ABC School")
                .attendanceStatus("PRESENT")
                .refreshmentStatus("PENDING")
                .eligible(true)
                .build();

        when(refreshmentService.getSessionRecipients(eq(101L), eq("ALL"), any())).thenReturn(List.of(recipient));

        mockMvc.perform(get("/api/refreshment/sessions/101/recipients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aman Singh"))
                .andExpect(jsonPath("$[0].attendanceStatus").value("PRESENT"))
                .andExpect(jsonPath("$[0].refreshmentStatus").value("PENDING"));
    }

    // =========================================================================
    // 5. DISTRIBUTION OPERATIONS
    // =========================================================================

    @Test
    @DisplayName("POST /api/refreshment/distributions/mark-given - 200 OK")
    void markGiven_Success() throws Exception {
        MarkGivenRequestDTO request = MarkGivenRequestDTO.builder()
                .sessionId(101L)
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .recipientId(501L)
                .remarks("Served at Counter 1")
                .build();

        DistributionResponseDTO response = DistributionResponseDTO.builder()
                .id(1001L)
                .sessionId(101L)
                .sessionName("SQL Master Lunch")
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .recipientId(501L)
                .recipientName("Aman Singh")
                .status(DistributionStatus.GIVEN)
                .distributedByName("Rahul Volunteer")
                .distributedAt(LocalDateTime.now())
                .build();

        when(refreshmentService.markGiven(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/refreshment/distributions/mark-given")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1001))
                .andExpect(jsonPath("$.recipientName").value("Aman Singh"))
                .andExpect(jsonPath("$.status").value("GIVEN"));
    }

    @Test
    @DisplayName("POST /api/refreshment/distributions/mark-given-bulk - 200 OK")
    void markGivenBulk_Success() throws Exception {
        MarkGivenBulkRequestDTO request = MarkGivenBulkRequestDTO.builder()
                .sessionId(101L)
                .recipientCategory(RecipientCategory.PARTICIPANT)
                .recipientIds(List.of(501L, 502L))
                .build();

        DistributionResponseDTO d1 = DistributionResponseDTO.builder().id(1001L).recipientId(501L).status(DistributionStatus.GIVEN).build();
        DistributionResponseDTO d2 = DistributionResponseDTO.builder().id(1002L).recipientId(502L).status(DistributionStatus.GIVEN).build();

        when(refreshmentService.markGivenBulk(any(), any())).thenReturn(List.of(d1, d2));

        mockMvc.perform(post("/api/refreshment/distributions/mark-given-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/refreshment/sessions/{sessionId}/guest-count/increment - 200 OK")
    void incrementGuestCount_Success() throws Exception {
        GuestCountIncrementDTO request = GuestCountIncrementDTO.builder()
                .incrementBy(5)
                .remarks("VIP lounge tea")
                .build();

        RefreshmentSessionResponseDTO session = RefreshmentSessionResponseDTO.builder()
                .id(105L)
                .name("Guest Tea")
                .trackingType(TrackingType.COUNT_BASED)
                .countDistributed(15)
                .build();

        when(refreshmentService.incrementGuestCount(eq(105L), any(), any())).thenReturn(session);

        mockMvc.perform(post("/api/refreshment/sessions/105/guest-count/increment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countDistributed").value(15));
    }

    // =========================================================================
    // 6. CORRECTION & AUDIT
    // =========================================================================

    @Test
    @DisplayName("POST /api/refreshment/distributions/{distributionId}/correct - 200 OK")
    void correctDistribution_Success() throws Exception {
        CorrectionRequestDTO request = CorrectionRequestDTO.builder()
                .reason("Marked mistakenly; participant had not arrived.")
                .build();

        DistributionResponseDTO response = DistributionResponseDTO.builder()
                .id(1001L)
                .status(DistributionStatus.CORRECTED_REVOKED)
                .correctionReason("Marked mistakenly; participant had not arrived.")
                .build();

        when(refreshmentService.correctDistribution(eq(1001L), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/refreshment/distributions/1001/correct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CORRECTED_REVOKED"))
                .andExpect(jsonPath("$.correctionReason").value("Marked mistakenly; participant had not arrived."));
    }

    @Test
    @DisplayName("GET /api/refreshment/distributions/{distributionId}/history - 200 OK")
    void getDistributionHistory_Success() throws Exception {
        DistributionResponseDTO dist = DistributionResponseDTO.builder()
                .id(1001L)
                .recipientName("Aman Singh")
                .status(DistributionStatus.GIVEN)
                .build();

        when(refreshmentService.getDistributionHistory(1001L)).thenReturn(List.of(dist));

        mockMvc.perform(get("/api/refreshment/distributions/1001/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1001));
    }

    // =========================================================================
    // 7. LOOKUP & DIRECTORY
    // =========================================================================

    @Test
    @DisplayName("GET /api/refreshment/recipients/lookup - 200 OK")
    void lookupRecipient_Success() throws Exception {
        RecipientLookupDTO lookup = RecipientLookupDTO.builder()
                .recipientId(501L)
                .name("Aman Singh")
                .schoolName("ABC School")
                .attendanceStatus("PRESENT")
                .build();

        when(refreshmentService.lookupRecipient(eq("Aman"), any())).thenReturn(List.of(lookup));

        mockMvc.perform(get("/api/refreshment/recipients/lookup?query=Aman"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aman Singh"));
    }

    @Test
    @DisplayName("GET /api/refreshment/recipients - 200 OK")
    void getGlobalRecipients_Success() throws Exception {
        RecipientDistributionDTO recipient = RecipientDistributionDTO.builder()
                .recipientId(501L)
                .name("Aman Singh")
                .refreshmentStatus("PENDING")
                .build();

        when(refreshmentService.getGlobalRecipients(any(), any(), any(), any(), any(), any())).thenReturn(List.of(recipient));

        mockMvc.perform(get("/api/refreshment/recipients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipientId").value(501));
    }

    // =========================================================================
    // 8. GUEST RECORDS
    // =========================================================================

    @Test
    @DisplayName("POST /api/refreshment/guests - 201 Created")
    void createGuest_Success() throws Exception {
        GuestRecordRequestDTO request = GuestRecordRequestDTO.builder()
                .planId(1L)
                .name("Chief Guest")
                .organization("State Education Board")
                .build();

        GuestRecordResponseDTO response = GuestRecordResponseDTO.builder()
                .id(201L)
                .name("Chief Guest")
                .organization("State Education Board")
                .build();

        when(refreshmentService.createGuestRecord(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/refreshment/guests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Chief Guest"));
    }

    @Test
    @DisplayName("GET /api/refreshment/plans/{planId}/guests - 200 OK")
    void getGuestsByPlan_Success() throws Exception {
        GuestRecordResponseDTO guest = GuestRecordResponseDTO.builder()
                .id(201L)
                .name("Chief Guest")
                .build();

        when(refreshmentService.getGuestsByPlan(1L)).thenReturn(List.of(guest));

        mockMvc.perform(get("/api/refreshment/plans/1/guests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(201));
    }

    @Test
    @DisplayName("DELETE /api/refreshment/guests/{id} - 204 No Content")
    void deleteGuest_Success() throws Exception {
        doNothing().when(refreshmentService).deleteGuestRecord(eq(201L), any());

        mockMvc.perform(delete("/api/refreshment/guests/201"))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // 9. OC DASHBOARD LIVE SUMMARY
    // =========================================================================

    @Test
    @DisplayName("GET /api/refreshment/dashboard/summary - 200 OK")
    void getDashboardSummary_Success() throws Exception {
        DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                .planId(1L)
                .planName("APEX 2026")
                .totalEligible(540L)
                .totalPresent(486L)
                .totalDistributed(452L)
                .totalPending(34L)
                .overallDistributionRate(93.0)
                .categoryWise(List.of(
                        CategorySummaryDTO.builder().category(RecipientCategory.PARTICIPANT).eligible(450L).present(400L).distributed(380L).pending(20L).distributionRate(95.0).build()
                ))
                .eventWise(List.of(
                        EventSummaryDTO.builder().eventId(10L).eventName("SQL Master").eligible(30L).present(26L).distributed(24L).pending(2L).distributionRate(92.3).build()
                ))
                .build();

        when(refreshmentService.getDashboardSummary(any())).thenReturn(summary);

        mockMvc.perform(get("/api/refreshment/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEligible").value(540))
                .andExpect(jsonPath("$.totalPresent").value(486))
                .andExpect(jsonPath("$.totalDistributed").value(452))
                .andExpect(jsonPath("$.totalPending").value(34))
                .andExpect(jsonPath("$.overallDistributionRate").value(93.0));
    }
}
