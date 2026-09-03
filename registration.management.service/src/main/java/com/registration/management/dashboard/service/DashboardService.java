package com.registration.management.dashboard.service;

import com.registration.management.auth.repository.PermissionRepository;
import com.registration.management.auth.repository.RoleRepository;
import com.registration.management.auth.repository.UserRepository;
import com.registration.management.dashboard.dto.DashboardStatsDTO;
import com.registration.management.school.repository.schoolRepository;
import com.registration.management.school.repository.schoolStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final schoolRepository schoolRepository;
    private final schoolStaffRepository schoolStaffRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getStats() {
        return DashboardStatsDTO.builder()
                .totalUsers(userRepository.count())
                .totalRoles(roleRepository.count())
                .totalPermissions(permissionRepository.count())
                .totalSchools(schoolRepository.count())
                .totalStaff(schoolStaffRepository.count())
                .build();
    }
}
