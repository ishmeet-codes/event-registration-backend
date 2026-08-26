package com.registration.management.auth.service;

import com.registration.management.auth.dto.AssignPermissionsRequest;
import com.registration.management.auth.dto.RoleRequest;
import com.registration.management.auth.dto.RoleResponse;

import java.util.List;

public interface RoleService {
    RoleResponse createRole(RoleRequest request);
    List<RoleResponse> getAllRoles();
    RoleResponse getRoleById(Long id);
    RoleResponse updateRole(Long id, RoleRequest request);
    void deleteRole(Long id);
    RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request);
}
