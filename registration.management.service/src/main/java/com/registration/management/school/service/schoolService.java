package com.registration.management.school.service;

import com.registration.management.auth.entities.User;
import com.registration.management.school.dto.schoolDTO;
import org.springframework.data.domain.Page;

public interface schoolService {

    schoolDTO createSchool(schoolDTO request, User currentUser);

    schoolDTO getSchoolById(Long schoolId, boolean includeSummary);

    Page<schoolDTO> getSchools(
            String search,
            String city,
            String district,
            String state,
            String board,
            Boolean active,
            int page,
            int size,
            String sort
    );
}
