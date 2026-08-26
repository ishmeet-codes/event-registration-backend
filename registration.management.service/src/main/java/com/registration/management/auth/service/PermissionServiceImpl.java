package com.registration.management.auth.service;

import com.registration.management.auth.dto.PermissionRequest;
import com.registration.management.auth.dto.PermissionResponse;
import com.registration.management.auth.entities.Permission;
import com.registration.management.auth.repository.PermissionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    // ─── Create ────────────────────────────────────────────────────────────────

    @Override
    public PermissionResponse createPermission(PermissionRequest request) {
        if (permissionRepository.existsByPermissionCode(request.getPermissionCode())) {
            throw new IllegalArgumentException("Permission code already exists: " + request.getPermissionCode());
        }

        Permission permission = Permission.builder()
                .permissionCode(request.getPermissionCode().toUpperCase())
                .permissionName(request.getPermissionName())
                .description(request.getDescription())
                .build();

        return toResponse(permissionRepository.save(permission));
    }

    // ─── Read ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ─── Update ─────────────────────────────────────────────────────────────────

    @Override
    public PermissionResponse updatePermission(Long id, PermissionRequest request) {
        Permission permission = findOrThrow(id);
        permission.setPermissionName(request.getPermissionName());
        permission.setDescription(request.getDescription());
        return toResponse(permissionRepository.save(permission));
    }

    // ─── Delete ─────────────────────────────────────────────────────────────────

    @Override
    public void deletePermission(Long id) {
        permissionRepository.delete(findOrThrow(id));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Permission findOrThrow(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + id));
    }

    private PermissionResponse toResponse(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .permissionCode(p.getPermissionCode())
                .permissionName(p.getPermissionName())
                .description(p.getDescription())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
