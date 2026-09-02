package com.registration.management.school.service;

import com.registration.management.auth.entities.User;
import com.registration.management.enums.StaffRole;
import com.registration.management.school.dto.schoolStaffDTO;
import org.springframework.data.domain.Page;

public interface schoolStaffService {

    schoolStaffDTO createStaff(Long schoolId, schoolStaffDTO request, User currentUser);

    Page<schoolStaffDTO> getStaffList(
            Long schoolId,
            String search,
            StaffRole staffRole,
            Boolean active,
            int page,
            int size,
            String sort
    );

    schoolStaffDTO getStaffById(Long schoolId, Long staffId);

    schoolStaffDTO updateStaff(Long schoolId, Long staffId, schoolStaffDTO request, User currentUser);

    schoolStaffDTO updateStaffStatus(Long schoolId, Long staffId, Boolean active, User currentUser);

    void deleteStaff(Long schoolId, Long staffId, User currentUser);
}
