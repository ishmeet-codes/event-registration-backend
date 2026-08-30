package com.registration.management.auth.service;

import com.registration.management.auth.dto.BulkImportResultDto;
import com.registration.management.auth.dto.UserRequestDto;
import com.registration.management.auth.dto.UserResponseDto;
import com.registration.management.auth.entities.RegisterUserRequest;
import com.registration.management.auth.entities.Role;
import com.registration.management.auth.entities.User;
import com.registration.management.auth.entities.UserResponse;
import com.registration.management.auth.repository.RoleRepository;
import com.registration.management.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserResponse registerUser(RegisterUserRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }
        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setRole(req.getRole());
        user.setActive(true);
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        User saved = userRepository.save(user);
        return new UserResponse(String.valueOf(saved.getId()), saved.getEmail(), saved.getRole());
    }

    @Override
    public UserResponseDto createUser(UserRequestDto request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }

        Role role = findRoleOrThrow(request.getRoleId());

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(true)
                .build();

        return toDto(userRepository.save(user));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<UserResponseDto> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDto);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        return toDto(findUserOrThrow(id));
    }

    @Override
    public UserResponseDto updateUser(Long id, UserRequestDto request) {
        User user = findUserOrThrow(id);
        user.setFullName(request.getFullName());
        if (request.getRoleId() != null) {
            user.setRole(findRoleOrThrow(request.getRoleId()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return toDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto setUserStatus(Long id, boolean active) {
        User user = findUserOrThrow(id);
        user.setActive(active);
        return toDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto changeUserRole(Long id, Long roleId) {
        User user = findUserOrThrow(id);
        user.setRole(findRoleOrThrow(roleId));
        return toDto(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id) {
        User user = findUserOrThrow(id);
        user.setActive(false);
        userRepository.save(user);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Bulk Import
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Expected CSV header (case-insensitive, trimmed):
     *   fullName, email, roleCode, password (optional)
     *
     * If password is blank/missing, a random secure password is generated.
     * Only one row's failure must not abort the whole batch.
     */
    @Override
    public BulkImportResultDto bulkImportUsers(MultipartFile file) {
        List<BulkImportResultDto.RowResult> rows = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // Parse header indices
            String[] headers = headerLine.split(",", -1);
            int idxFullName = -1, idxEmail = -1, idxRoleCode = -1, idxPassword = -1;
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().toLowerCase();
                if (h.equals("fullname") || h.equals("full_name") || h.equals("name")) idxFullName = i;
                else if (h.equals("email"))    idxEmail    = i;
                else if (h.equals("rolecode") || h.equals("role_code") || h.equals("role")) idxRoleCode = i;
                else if (h.equals("password")) idxPassword = i;
            }

            if (idxFullName < 0 || idxEmail < 0) {
                throw new IllegalArgumentException(
                        "CSV must have at least 'fullName' and 'email' columns");
            }

            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;

                String[] cols = line.split(",", -1);
                String fullName = safeGet(cols, idxFullName).trim();
                String email    = safeGet(cols, idxEmail).trim();
                String roleCode = idxRoleCode >= 0 ? safeGet(cols, idxRoleCode).trim() : "PARTICIPANT";
                String password = idxPassword >= 0 ? safeGet(cols, idxPassword).trim() : "";

                if (roleCode.isBlank()) roleCode = "PARTICIPANT";
                if (password.isBlank()) password = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

                BulkImportResultDto.RowResult.RowResultBuilder result =
                        BulkImportResultDto.RowResult.builder()
                                .rowNumber(rowNum)
                                .email(email)
                                .fullName(fullName)
                                .roleCode(roleCode);

                try {
                    if (fullName.isBlank()) throw new IllegalArgumentException("fullName is required");
                    if (email.isBlank())    throw new IllegalArgumentException("email is required");

                    if (userRepository.findByEmail(email).isPresent()) {
                        throw new IllegalArgumentException("Email already exists: " + email);
                    }

                    Role role = roleRepository.findByRoleCode(roleCode)
                            .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + roleCode));

                    User user = User.builder()
                            .fullName(fullName)
                            .email(email)
                            .password(passwordEncoder.encode(password))
                            .role(role)
                            .active(true)
                            .build();

                    User saved = userRepository.save(user);
                    rows.add(result.userId(saved.getId()).success(true).build());
                    successCount++;

                } catch (Exception ex) {
                    rows.add(result.success(false).error(ex.getMessage()).build());
                    failureCount++;
                }
            }

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }

        return BulkImportResultDto.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .rows(rows)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private String safeGet(String[] arr, int idx) {
        return (idx >= 0 && idx < arr.length) ? arr[idx] : "";
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    private Role findRoleOrThrow(Long roleId) {
        if (roleId == null) throw new IllegalArgumentException("roleId is required");
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));
    }

    private UserResponseDto toDto(User u) {
        return UserResponseDto.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .active(u.isActive())
                .roleCode(u.getRole() != null ? u.getRole().getRoleCode() : null)
                .roleName(u.getRole() != null ? u.getRole().getRoleName() : null)
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
