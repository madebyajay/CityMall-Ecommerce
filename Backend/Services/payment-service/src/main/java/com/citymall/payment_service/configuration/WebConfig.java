package com.citymall.payment_service.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS for local frontend dev (Vite :5173) + future storefront origins.
 * Override via env FRONTEND_ORIGINS (comma-separated) in prod.
 * Payment-service has no Spring Security, so CORS is configured at MVC level.
 * Mirrors customer-service so browser calls from the SPA are accepted.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String origins = System.getenv().getOrDefault(
                "FRONTEND_ORIGINS", "http://localhost:5173,http://localhost:3000");
        registry.addMapping("/**")
                .allowedOrigins(origins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
