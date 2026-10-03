package com.citymall.controller;

import com.citymall.dto.auth.AuthResponse;
import com.citymall.dto.auth.LoginRequest;
import com.citymall.dto.auth.OtpRequest;
import com.citymall.dto.auth.RefreshTokenRequest;
import com.citymall.dto.auth.RegisterRequest;
import com.citymall.dto.auth.ResetPasswordRequest;
import com.citymall.dto.user.UserResponse;
import com.citymall.response.ApiResponse;
import com.citymall.security.CustomUserDetails;
import com.citymall.service.AuthService;
import com.citymall.service.LoginService;
import com.citymall.service.OtpService;
import com.citymall.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public auth surface — register, login (single-session with forced-login),
 * refresh rotation, logout (session revoke), OTP and OTP-gated password reset.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginService loginService;
    private final OtpService otpService;
    private final UserService userService;

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

    /**
     * Shared login for CUSTOMER + SELLER + ADMIN + SUPERADMIN (same users table).
     * Rejected with 409-style error when another device holds the session unless
     * {@code forcedLogin=true}.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse auth = loginService.login(request, AuthService.clientIp(httpRequest));
        String msg = auth.forced()
                ? "Login successful (previous device session ended)"
                : "Login successful";
        return ok(msg, auth);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ok("Token refreshed successfully", authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        loginService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Logout successful. Session revoked.")
                .statusCode("200")
                .success(true)
                .build());
    }

    /** Frontend route-guard contract: profile + role of the caller. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ok("Profile fetched successfully",
                userService.getByLoginId(principal.getUsername()));
    }

    /** Lightweight token check for service-to-service / gateway health. */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<String>> validate() {
        return ok("Authentication Success", "valid");
    }

    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<String>> requestOtp(@Valid @RequestBody OtpRequest request) {
        return ok(otpService.requestOtp(request.identifier().trim()), request.identifier().trim());
    }

    /** Pre-login UX gate — surfaces lockout state before attempting login. */
    @GetMapping("/check-block")
    public ResponseEntity<ApiResponse<UserResponse>> checkBlock(@RequestParam String identifier) {
        UserResponse user = userService.getByLoginIdentifier(identifier.trim());
        String msg = Boolean.FALSE.equals(user.activeStatus())
                ? "Account is blocked/disabled" : "Account status fetched";
        return ok(msg, user);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Password reset successfully. Please login again.")
                .statusCode("200")
                .success(true)
                .build());
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.<T>builder()
                .message(message).statusCode("200").success(true).data(data).build());
    }
}
