package com.citymall.service;

import com.citymall.dto.auth.AuthResponse;
import com.citymall.dto.auth.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Thin facade over {@link AuthService} so that login/logout has its own
 * service + controller surface (as requested). Business rules stay in AuthService.
 *
 * <p>Logout is stateless today (client drops tokens). Next phase (Redis):
 * blacklist access-token jti until expiry + remove refresh-token from allowlist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final AuthService authService;

    public AuthResponse login(LoginRequest req, String clientIp) {
        return authService.authenticate(req, clientIp);
    }

    public void logout(String authHeader) {
        // No-op until Redis blacklist lands. Kept as explicit seam.
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.info("Logout requested (token will be blacklisted once Redis lands)");
        } else {
            log.info("Logout requested without token");
        }
    }
}
