package com.registration.management.report.controller;

import com.registration.management.report.dto.*;
import com.registration.management.report.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();
    }

    @Test
    @DisplayName("GET /api/reports/dashboard returns 200 OK with DashboardReportDTO")
    void getDashboardReport_ReturnsOk() throws Exception {
        DashboardReportDTO dto = DashboardReportDTO.builder()
                .totalRegistrations(10)
                .totalParticipants(25)
                .totalCheckins(15)
                .totalSchools(5)
                .totalEvents(3)
                .build();

        when(reportService.getDashboardReport(any())).thenReturn(dto);

        mockMvc.perform(get("/api/reports/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRegistrations").value(10))
                .andExpect(jsonPath("$.totalParticipants").value(25))
                .andExpect(jsonPath("$.totalCheckins").value(15));
    }

    @Test
    @DisplayName("GET /api/reports/overview returns 200 OK")
    void getOverviewReport_ReturnsOk() throws Exception {
        OverviewReportDTO dto = OverviewReportDTO.builder()
                .totalRegistrations(10)
                .overallAttendanceRate(85.5)
                .build();

        when(reportService.getOverviewReport(any(), any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/reports/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRegistrations").value(10))
                .andExpect(jsonPath("$.overallAttendanceRate").value(85.5));
    }

    @Test
    @DisplayName("GET /api/reports/export returns CSV attachment")
    void exportReport_ReturnsCsv() throws Exception {
        byte[] csvData = "ID,Registration Number\n1,REG-001\n".getBytes();
        when(reportService.exportReport(any(), any(), any(), any(), any())).thenReturn(csvData);

        mockMvc.perform(get("/api/reports/export?type=REGISTRATION&format=csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("report_registration")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("REG-001")));
    }
}
