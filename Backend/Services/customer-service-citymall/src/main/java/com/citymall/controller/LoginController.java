package com.citymall.controller;

import com.citymall.dto.auth.AuthResponse;
import com.citymall.dto.auth.LoginRequest;
import com.citymall.response.ApiResponse;
import com.citymall.service.AuthService;
import com.citymall.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Dedicated login/logout surface (shares logic with AuthService).
 * POST /api/v1/login  -> verify credentials, issue JWT pair
 * POST /api/v1/logout -> stateless ack today; Redis blacklist hooks in next phase
 */
@RestController
@RequestMapping("/auth/v2")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginUser(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse auth = loginService.login(request, AuthService.clientIp(httpRequest));
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .message("Login successful")
                        .statusCode("200")
                        .success(true)
                        .data(auth)
                        .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        loginService.logout(authHeader);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Logout successful. Please discard tokens client-side.")
                        .statusCode("200")
                        .success(true)
                        .data(null)
                        .build());
    }
}
