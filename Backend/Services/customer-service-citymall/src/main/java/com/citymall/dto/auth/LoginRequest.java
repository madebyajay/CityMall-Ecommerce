package com.citymall.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        /** Accepts userLoginId OR email OR mobile number as string. */
        @NotBlank(message = "Login identifier (loginId/email/mobile) is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password,

        /**
         * When another device holds an active session, login is rejected with 409
         * unless {@code forcedLogin=true} (kicks the other device).
         */
        Boolean forcedLogin
) {
}
