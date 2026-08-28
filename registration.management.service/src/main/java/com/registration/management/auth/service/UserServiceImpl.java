package com.registration.management.auth.service;

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
