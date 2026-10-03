package com.citymall.controller;

import com.citymall.dto.admin.BlockCustomerRequest;
import com.citymall.dto.admin.CreateAdminRequest;
import com.citymall.dto.admin.UpdateUserRoleRequest;
import com.citymall.dto.permission.AssignPermissionsRequest;
import com.citymall.dto.permission.CreatePermissionRequest;
import com.citymall.dto.permission.PermissionResponse;
import com.citymall.dto.user.UserResponse;
import com.citymall.response.ApiResponse;
import com.citymall.service.PermissionService;
import com.citymall.service.SuperAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SUPERADMIN-only surface — full user directory, role assignment, permission
 * master and destructive deletes. Mirrors the reference user-management
 * {@code SuperAdminUserController} + {@code RoleModuleController}, trimmed for
 * CityMall (no commissions/courts/PanIndia).
 */
@RestController
@RequestMapping("/api/v1/superadmin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final PermissionService permissionService;

    @PostMapping("/create-superadmin")
    public ResponseEntity<ApiResponse<UserResponse>> createSuperAdmin(
            @Valid @RequestBody CreateAdminRequest request) {
        UserResponse created = superAdminService.createSuperAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserResponse>builder()
                        .message("SuperAdmin created successfully")
                        .statusCode("201")
                        .success(true)
                        .data(created)
                        .build());
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<UserResponse> page = superAdminService.searchUsers(query, role, pageable);
        String msg = page.isEmpty() ? "No users found" : "Users fetched successfully";
        return ResponseEntity.ok(ApiResponse.<Page<UserResponse>>builder()
                .message(msg).statusCode("200").success(true).data(page).build());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .message("User fetched successfully")
                .statusCode("200").success(true).data(superAdminService.getUser(id)).build());
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .message("User role updated successfully")
                .statusCode("200").success(true)
                .data(superAdminService.updateUserRole(id, request.roleName())).build());
    }

    @PatchMapping("/users/{id}/block")
    public ResponseEntity<ApiResponse<UserResponse>> setBlocked(
            @PathVariable Long id,
            @Valid @RequestBody BlockCustomerRequest request) {
        UserResponse updated = superAdminService.setBlocked(
                id, Boolean.TRUE.equals(request.blocked()), request.reason());
        String msg = Boolean.TRUE.equals(request.blocked())
                ? "User blocked successfully" : "User unblocked successfully";
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .message(msg).statusCode("200").success(true).data(updated).build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        superAdminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("User deleted successfully").statusCode("200").success(true).build());
    }

    // ---------------- permission master ----------------

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> listPermissions() {
        List<PermissionResponse> permissions = permissionService.listAll();
        return ResponseEntity.ok(ApiResponse.<List<PermissionResponse>>builder()
                .message(permissions.isEmpty() ? "No permissions found" : "Permissions fetched successfully")
                .statusCode("200").success(true).data(permissions).build());
    }

    @PostMapping("/permissions")
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
            @Valid @RequestBody CreatePermissionRequest request) {
        PermissionResponse created = permissionService.create(
                request.module(), request.apiEndpoint(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<PermissionResponse>builder()
                        .message("Permission created successfully")
                        .statusCode("201").success(true).data(created).build());
    }

    @PostMapping("/permissions/assign")
    public ResponseEntity<ApiResponse<Void>> assignPermissions(
            @Valid @RequestBody AssignPermissionsRequest request) {
        permissionService.assignPermissions(request.roleName(), request.permissionIds());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Permissions assigned successfully")
                .statusCode("200").success(true).build());
    }

    @DeleteMapping("/permissions")
    public ResponseEntity<ApiResponse<Void>> revokePermission(
            @RequestParam String roleName,
            @RequestParam String apiEndpoint) {
        permissionService.revokePermission(roleName, apiEndpoint);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Permission revoked successfully")
                .statusCode("200").success(true).build());
    }
}
