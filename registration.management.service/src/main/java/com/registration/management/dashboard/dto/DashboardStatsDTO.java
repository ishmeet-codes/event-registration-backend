package com.registration.management.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDTO {
    private long totalUsers;
    private long totalRoles;
    private long totalPermissions;
    private long totalSchools;
    private long totalStaff;
}
