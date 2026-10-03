package com.citymall.config;

import com.citymall.security.JwtAuthenticationEntryPoint;
import com.citymall.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;
    private final JwtAuthenticationEntryPoint point;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.OPTIONS, "/**").permitAll()
                        // Public: registration + login/logout + token refresh + OTP + health
                        .requestMatchers("/api/v1/auth/**")
                        .permitAll()
                        .requestMatchers("/actuator/**", "/error")
                        .permitAll()
                        // Service-to-service (order-service Feign). Shared-secret
                        // enforced inside JwtAuthenticationFilter.
                        .requestMatchers("/api/v1/internal/**")
                        .permitAll()
                        // SuperAdmin surface: SUPERADMIN only.
                        .requestMatchers("/api/v1/superadmin/**")
                        .hasRole("SUPERADMIN")
                        // Admin surface: ADMIN or SUPERADMIN (URL level; method
                        // @PreAuthorize below is defence-in-depth).
                        // CUSTOMER JWT -> 403 via accessDeniedHandler; no token -> 401.
                        .requestMatchers("/api/v1/admin/**")
                        .hasAnyRole("ADMIN", "SUPERADMIN")
                        // Role reads need auth; writes need ADMIN or SUPERADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/roles/**")
                        .authenticated()
                        .requestMatchers("/api/v1/roles/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        // Everything else (users) needs a valid access token
                        .anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(point)
                        .accessDeniedHandler((req, res, ex) -> writeError(res, HttpStatus.FORBIDDEN,
                                "Forbidden: " + ex.getMessage())))

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    /**
     * CORS for local frontend dev (Vite :5173) + future storefront origins.
     * Override via env FRONTEND_ORIGINS (comma-separated) in prod.
     * Uses Authorization header (Bearer JWT), so no cookies needed.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        String origins = System.getenv().getOrDefault(
                "FRONTEND_ORIGINS", "http://localhost:5173,http://localhost:3000");
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }





    private void writeError(jakarta.servlet.http.HttpServletResponse res, HttpStatus status, String message)
            throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("message", message);
        body.put("statusCode", String.valueOf(status.value()));
        body.put("success", false);
        body.put("data", null);
        body.put("timestamp", LocalDateTime.now().toString());
        objectMapper.writeValue(res.getOutputStream(), body);
    }
}
