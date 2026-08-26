package com.registration.management.auth.service;

import com.registration.management.auth.dto.AssignPermissionsRequest;
import com.registration.management.auth.dto.PermissionResponse;
import com.registration.management.auth.dto.RoleRequest;
import com.registration.management.auth.dto.RoleResponse;
import com.registration.management.auth.entities.Permission;
import com.registration.management.auth.entities.Role;
import com.registration.management.auth.entities.RolePermission;
import com.registration.management.auth.repository.PermissionRepository;
import com.registration.management.auth.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    // ─── Create ────────────────────────────────────────────────────────────────

    @Override
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.findByRoleCode(request.getRoleCode()).isPresent()) {
            throw new IllegalArgumentException("Role code already exists: " + request.getRoleCode());
        }

        Role role = Role.builder()
                .roleCode(request.getRoleCode().toUpperCase())
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .systemRole(request.getSystemRole() != null && request.getSystemRole())
                .active(request.getActive() == null || request.getActive())
                .build();

        return toResponse(roleRepository.save(role));
    }

    // ─── Read ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        return toResponse(findRoleOrThrow(id));
    }

    // ─── Update ─────────────────────────────────────────────────────────────────

    @Override
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = findRoleOrThrow(id);
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        if (request.getActive() != null) {
            role.setActive(request.getActive());
        }
        return toResponse(roleRepository.save(role));
    }

    // ─── Delete ─────────────────────────────────────────────────────────────────

    @Override
    public void deleteRole(Long id) {
        Role role = findRoleOrThrow(id);
        if (Boolean.TRUE.equals(role.getSystemRole())) {
            throw new IllegalStateException("Cannot delete a system role: " + role.getRoleCode());
        }
        roleRepository.delete(role);
    }

    // ─── Assign Permissions ─────────────────────────────────────────────────────

    @Override
    public RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request) {
        Role role = findRoleOrThrow(roleId);

        // Remove existing permissions not in the new list (full replace)
        role.getRolePermissions().clear();

        for (Long permId : request.getPermissionIds()) {
            Permission permission = permissionRepository.findById(permId)
                    .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + permId));

            RolePermission rp = RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .build();
            role.getRolePermissions().add(rp);
        }

        return toResponse(roleRepository.save(role));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Role findRoleOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + id));
    }

    private RoleResponse toResponse(Role role) {
        List<PermissionResponse> permissions = role.getRolePermissions().stream()
                .map(rp -> PermissionResponse.builder()
                        .id(rp.getPermission().getId())
                        .permissionCode(rp.getPermission().getPermissionCode())
                        .permissionName(rp.getPermission().getPermissionName())
                        .description(rp.getPermission().getDescription())
                        .createdAt(rp.getPermission().getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return RoleResponse.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .systemRole(role.getSystemRole())
                .active(role.getActive())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .permissions(permissions)
                .build();
    }
}
