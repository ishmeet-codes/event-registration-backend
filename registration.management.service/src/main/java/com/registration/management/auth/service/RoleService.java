package com.registration.management.auth.service;

import com.registration.management.auth.dto.AssignPermissionsRequest;
import com.registration.management.auth.dto.PermissionResponse;
import com.registration.management.auth.dto.RoleRequest;
import com.registration.management.auth.dto.RoleResponse;

import java.util.List;

public interface RoleService {
    RoleResponse createRole(RoleRequest request);
    List<RoleResponse> getAllRoles();
    RoleResponse getRoleById(Long id);
    RoleResponse updateRole(Long id, RoleRequest request);
    RoleResponse setRoleStatus(Long id, boolean active);
    void deleteRole(Long id);
    RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request);
    List<PermissionResponse> getRolePermissions(Long roleId);
    RoleResponse addPermission(Long roleId, Long permissionId);
    RoleResponse removePermission(Long roleId, Long permissionId);
}
