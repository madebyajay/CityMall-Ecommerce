package com.citymall.controller;

import com.citymall.dto.address.AddressRequest;
import com.citymall.dto.address.AddressResponse;
import com.citymall.dto.user.ChangePasswordRequest;
import com.citymall.dto.user.UpdateUserRequest;
import com.citymall.dto.user.UserResponse;
import com.citymall.response.ApiResponse;
import com.citymall.security.CustomUserDetails;
import com.citymall.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer profile resource — self-service plus ADMIN/SUPERADMIN oversight.
 * Hard deletes are SUPERADMIN-only (see {@code /api/v1/superadmin}).
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ok("Profile fetched successfully",
                userService.getByLoginId(principal.getUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ok("User fetched successfully", userService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<UserResponse> page = userService.list(pageable);
        String msg = page.isEmpty() ? "No users found" : "Users fetched successfully";
        return ResponseEntity.ok(ApiResponse.<Page<UserResponse>>builder()
                .message(msg).statusCode("200").success(true).data(page).build());
    }

    @GetMapping("/exists/{id}")
    public ResponseEntity<ApiResponse<Boolean>> exists(@PathVariable Long id) {
        boolean exists = userService.exists(id);
        return ok(exists ? "User exists with id: " + id : "User does not exist with id: " + id, exists);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ok("User updated successfully", userService.update(id, request));
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Password changed successfully. Please login again.")
                .statusCode("200").success(true).build());
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("User deactivated successfully").statusCode("200").success(true).build());
    }

    // ---------------- delivery addresses ----------------

    @GetMapping("/{id}/addresses")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> listAddresses(@PathVariable Long id) {
        List<AddressResponse> addresses = userService.listAddresses(id);
        return ok(addresses.isEmpty() ? "No addresses found" : "Addresses fetched successfully", addresses);
    }

    @PostMapping("/{id}/addresses")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse created = userService.addAddress(id, request);
        return ResponseEntity.status(201).body(ApiResponse.<AddressResponse>builder()
                .message("Address added successfully").statusCode("201").success(true).data(created).build());
    }

    @PutMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long id,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return ok("Address updated successfully", userService.updateAddress(id, addressId, request));
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long id,
            @PathVariable Long addressId) {
        userService.deleteAddress(id, addressId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Address removed successfully").statusCode("200").success(true).build());
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.<T>builder()
                .message(message).statusCode("200").success(true).data(data).build());
    }
}
