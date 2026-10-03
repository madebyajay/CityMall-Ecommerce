package com.citymall.controller;

import com.citymall.dto.internal.CustomerSummary;
import com.citymall.entity.User;
import com.citymall.exception.ResourceNotFound;
import com.citymall.repository.UserRepository;
import com.citymall.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service endpoints for order-service OpenFeign
 * ({@code customer-url: http://localhost:8090/api/v1/internal/users}).
 * No user JWT needed — shared-secret gate lives in
 * {@code JwtAuthenticationFilter} ({@code X-Internal-Secret}).
 */
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerSummary>> findUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("User not found with id: " + id));
        return ResponseEntity.ok(ApiResponse.<CustomerSummary>builder()
                .message("User fetched successfully")
                .statusCode("200").success(true)
                .data(CustomerSummary.from(user)).build());
    }

    @GetMapping("/exists/{id}")
    public ResponseEntity<ApiResponse<Boolean>> exists(@PathVariable Long id) {
        boolean exists = userRepository.existsById(id);
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .message(exists ? "User exists" : "User does not exist")
                .statusCode("200").success(true).data(exists).build());
    }
}
