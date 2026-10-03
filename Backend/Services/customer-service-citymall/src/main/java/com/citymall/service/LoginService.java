package com.citymall.service;

import com.citymall.dto.auth.AuthResponse;
import com.citymall.dto.auth.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Login/logout facade over {@link AuthService} — keeps a dedicated seam for
 * session handling (now Redis-backed) without duplicating business rules.
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
        authService.logout(authHeader);
    }
}
