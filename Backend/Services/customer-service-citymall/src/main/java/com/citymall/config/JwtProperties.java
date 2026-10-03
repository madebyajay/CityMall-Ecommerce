package com.citymall.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds to:
 * app.jwt.secret, app.jwt.access-expiration-ms, app.jwt.refresh-expiration-ms
 * Configure via config-server customer-service.yml (preferred) or env JWT_SECRET.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Base64-encoded HS256 secret (min 256 bits). */
    private String secret;

    /** Default 15 minutes. */
    private long accessExpirationMs = 900_000L;

    /** Default 7 days. */
    private long refreshExpirationMs = 604_800_000L;

    /** Max failed logins before temporary lock. */
    private int maxFailedAttempts = 5;

    /** Lock duration after max failures (minutes). */
    private long lockDurationMinutes = 15L;
}
