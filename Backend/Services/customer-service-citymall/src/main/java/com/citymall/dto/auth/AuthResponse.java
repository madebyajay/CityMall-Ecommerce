package com.citymall.dto.auth;

import com.citymall.dto.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {
    public static AuthResponse bearer(String access, String refresh, long expiresInMs, UserResponse user) {
        return new AuthResponse(access, refresh, "Bearer", expiresInMs, user);
    }
}
