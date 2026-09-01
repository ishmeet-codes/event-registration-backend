package com.registration.management.school.service;

import com.registration.management.auth.entities.User;
import com.registration.management.school.dto.schoolDTO;

public interface schoolService {

    schoolDTO createSchool(schoolDTO request, User currentUser);
}
