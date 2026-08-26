package com.registration.management.auth.service;

import com.registration.management.auth.dto.PermissionRequest;
import com.registration.management.auth.dto.PermissionResponse;

import java.util.List;

public interface PermissionService {
    PermissionResponse createPermission(PermissionRequest request);
    List<PermissionResponse> getAllPermissions();
    PermissionResponse getPermissionById(Long id);
    PermissionResponse updatePermission(Long id, PermissionRequest request);
    void deletePermission(Long id);
}
