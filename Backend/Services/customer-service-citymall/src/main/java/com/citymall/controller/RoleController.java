package com.citymall.controller;

import com.citymall.dto.role.RoleRequest;
import com.citymall.dto.role.RoleResponse;
import com.citymall.response.ApiResponse;
import com.citymall.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Role master-data module. Reads: any authenticated user. Writes: ADMIN only
 * (enforced in SecurityConfig + @PreAuthorize for defence in depth).
 */
@RestController
@RequestMapping("/auth/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAll() {
        List<RoleResponse> roles = roleService.getAll();
        return ResponseEntity.ok(ApiResponse.<List<RoleResponse>>builder()
                .message(roles.isEmpty() ? "No roles found" : "Roles fetched successfully")
                .statusCode("200").success(true).data(roles).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .message("Role fetched successfully")
                .statusCode("200").success(true).data(roleService.getById(id)).build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest request) {
        RoleResponse created = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<RoleResponse>builder()
                .message("Role created successfully")
                .statusCode("201").success(true).data(created).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.<RoleResponse>builder()
                .message("Role updated successfully")
                .statusCode("200").success(true).data(roleService.update(id, request)).build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> setActive(
            @PathVariable Long id, @RequestParam boolean active) {
        roleService.setActive(id, active);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message(active ? "Role activated" : "Role deactivated")
                .statusCode("200").success(true).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
