package com.citymall.service;

import com.citymall.dto.permission.PermissionResponse;
import com.citymall.entity.Permission;
import com.citymall.entity.Role;
import com.citymall.entity.RolePermission;
import com.citymall.exception.BadRequestException;
import com.citymall.exception.ResourceNotFound;
import com.citymall.repository.PermissionRepository;
import com.citymall.repository.RolePermissionRepository;
import com.citymall.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Minimal RBAC permission service (ported from user-management
 * {@code master_permissions + mapping_role_having_permissions} with the
 * 1-day {@code ROLE_PERMISSION:<ROLE>} Redis cache). SUPERADMIN bypasses
 * every check and needs no explicit mappings.
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final RedisSessionService sessions;

    @Transactional(readOnly = true)
    public List<String> endpointsForRole(String roleName) {
        if (roleName == null) return List.of();
        String role = roleName.toUpperCase();
        if ("SUPERADMIN".equals(role)) return List.of("*");
        List<String> cached = sessions.getCachedPermissions(role);
        if (cached != null) return cached;
        List<String> endpoints = rolePermissionRepository.findActiveEndpointsByRole(role);
        sessions.cachePermissions(role, endpoints);
        return endpoints;
    }

    public boolean hasPermission(String roleName, String path) {
        if (roleName == null || path == null) return false;
        if ("SUPERADMIN".equalsIgnoreCase(roleName)) return true;
        List<String> endpoints = endpointsForRole(roleName);
        if (endpoints.contains("*")) return true;
        for (String endpoint : endpoints) {
            if (path.equals(endpoint) || (endpoint.endsWith("/**")
                    && path.startsWith(endpoint.substring(0, endpoint.length() - 3)))) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listAll() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionResponse(
                        p.getId(), p.getModule(), p.getApiEndpoint(),
                        p.getDescription(), p.getActiveStatus()))
                .toList();
    }

    @Transactional
    public PermissionResponse create(String module, String apiEndpoint, String description) {
        if (permissionRepository.findByApiEndpoint(apiEndpoint).isPresent()) {
            throw new BadRequestException("Permission already exists: " + apiEndpoint);
        }
        Permission saved = permissionRepository.save(Permission.builder()
                .module(module)
                .apiEndpoint(apiEndpoint)
                .description(description)
                .activeStatus(true)
                .build());
        return new PermissionResponse(
                saved.getId(), saved.getModule(), saved.getApiEndpoint(),
                saved.getDescription(), saved.getActiveStatus());
    }

    /** Grant a set of permission ids to a role (idempotent, evicts the cache). */
    @Transactional
    public void assignPermissions(String roleName, List<Long> permissionIds) {
        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFound("Role not found: " + roleName));
        for (Long permissionId : permissionIds) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new ResourceNotFound("Permission not found: " + permissionId));
            rolePermissionRepository.findByRole_NameAndPermission_ApiEndpoint(
                            role.getName(), permission.getApiEndpoint())
                    .orElseGet(() -> rolePermissionRepository.save(RolePermission.builder()
                            .role(role)
                            .permission(permission)
                            .activeStatus(true)
                            .build()));
        }
        sessions.evictPermissions(role.getName());
    }

    @Transactional
    public void revokePermission(String roleName, String apiEndpoint) {
        rolePermissionRepository.deleteByRole_NameAndPermission_ApiEndpoint(
                roleName.toUpperCase(), apiEndpoint);
        sessions.evictPermissions(roleName);
    }
}
