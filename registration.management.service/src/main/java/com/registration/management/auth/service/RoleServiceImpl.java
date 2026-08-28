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

    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;

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

    @Override
    public RoleResponse setRoleStatus(Long id, boolean active) {
        Role role = findRoleOrThrow(id);
        role.setActive(active);
        return toResponse(roleRepository.save(role));
    }

    @Override
    public void deleteRole(Long id) {
        Role role = findRoleOrThrow(id);
        if (Boolean.TRUE.equals(role.getSystemRole())) {
            throw new IllegalStateException("Cannot delete a system role: " + role.getRoleCode());
        }
        roleRepository.delete(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getRolePermissions(Long roleId) {
        Role role = findRoleOrThrow(roleId);
        return role.getRolePermissions().stream()
                .map(rp -> toPermissionResponse(rp.getPermission()))
                .collect(Collectors.toList());
    }

    @Override
    public RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request) {
        Role role = findRoleOrThrow(roleId);
        // Full replace
        role.getRolePermissions().clear();

        for (Long permId : request.getPermissionIds()) {
            Permission permission = permissionRepository.findById(permId)
                    .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + permId));
            role.getRolePermissions().add(RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .build());
        }

        return toResponse(roleRepository.save(role));
    }

    @Override
    public RoleResponse addPermission(Long roleId, Long permissionId) {
        Role role = findRoleOrThrow(roleId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + permissionId));

        boolean alreadyAssigned = role.getRolePermissions().stream()
                .anyMatch(rp -> rp.getPermission().getId().equals(permissionId));

        if (!alreadyAssigned) {
            role.getRolePermissions().add(RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .build());
            roleRepository.save(role);
        }

        return toResponse(role);
    }

    @Override
    public RoleResponse removePermission(Long roleId, Long permissionId) {
        Role role = findRoleOrThrow(roleId);
        role.getRolePermissions().removeIf(rp -> rp.getPermission().getId().equals(permissionId));
        return toResponse(roleRepository.save(role));
    }

    private Role findRoleOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + id));
    }

    private PermissionResponse toPermissionResponse(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .permissionCode(p.getPermissionCode())
                .permissionName(p.getPermissionName())
                .description(p.getDescription())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private RoleResponse toResponse(Role role) {
        List<PermissionResponse> permissions = role.getRolePermissions().stream()
                .map(rp -> toPermissionResponse(rp.getPermission()))
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
