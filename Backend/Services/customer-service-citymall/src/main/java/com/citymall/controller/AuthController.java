package com.citymall.controller;

import com.citymall.dto.auth.AuthResponse;
import com.citymall.dto.auth.RefreshTokenRequest;
import com.citymall.dto.auth.RegisterRequest;
import com.citymall.response.ApiResponse;
import com.citymall.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse auth = authService.register(request, AuthService.clientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<AuthResponse>builder()
                        .message("User registered successfully")
                        .statusCode("201")
                        .success(true)
                        .data(auth)
                        .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse auth = authService.refresh(request);
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .message("Token refreshed successfully")
                        .statusCode("200")
                        .success(true)
                        .data(auth)
                        .build());
    }
}
