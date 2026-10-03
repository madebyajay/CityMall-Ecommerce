package com.citymall.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds {@code app.jwt.secret}. MUST match customer-service
 * (config-server {@code customer-service.yml} / {@code product-service.yml},
 * both defaulting to {@code ${JWT_SECRET:...}} with the same fallback).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class AdminJwtProperties {

    /** Base64-encoded HS256 secret (min 256 bits). */
    private String secret;
}
