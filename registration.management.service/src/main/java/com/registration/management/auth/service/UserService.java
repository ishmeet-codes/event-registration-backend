package com.registration.management.auth.service;

import com.registration.management.auth.dto.BulkImportResultDto;
import com.registration.management.auth.dto.UserRequestDto;
import com.registration.management.auth.dto.UserResponseDto;
import com.registration.management.auth.entities.RegisterUserRequest;
import com.registration.management.auth.entities.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface UserService {

    UserResponse registerUser(RegisterUserRequest registerUserRequest);

    UserResponseDto createUser(UserRequestDto request);
    Page<UserResponseDto> listUsers(Pageable pageable);
    UserResponseDto getUserById(Long id);
    UserResponseDto updateUser(Long id, UserRequestDto request);
    UserResponseDto setUserStatus(Long id, boolean active);
    UserResponseDto changeUserRole(Long id, Long roleId);
    void deleteUser(Long id);

    /** Parse a CSV file and bulk-create users. Returns per-row results. */
    BulkImportResultDto bulkImportUsers(MultipartFile file);
}
