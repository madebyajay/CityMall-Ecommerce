package com.citymall.security;

import com.citymall.config.InternalProperties;
import com.citymall.service.RedisSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads Bearer token, validates it and populates SecurityContext.
 *
 * <p>Single-session enforcement: when Redis holds a *different* access token
 * for the token's user (kicked device / logout / password change), this token
 * no longer authenticates. Refresh tokens never authorize API calls.
 *
 * <p>Internal service endpoints ({@code /api/v1/internal/**}) carry
 * {@code X-Internal-Secret} instead of a JWT when a secret is configured.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PREFIX = "/api/v1/internal/";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RedisSessionService sessions;
    private final InternalProperties internalProps;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().contains(INTERNAL_PREFIX)) {
            String configured = internalProps.getSecret();
            if (configured != null && !configured.isBlank()
                    && !configured.equals(request.getHeader(INTERNAL_SECRET_HEADER))) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal secret");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        // Refresh tokens must never authorize API calls
        if (!jwtService.isValid(token) || jwtService.isRefreshToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = jwtService.extractUserId(token);
            if (userId != null && !sessions.isAccessTokenActive(userId, token)) {
                // Kicked by a newer login, or logged out — token is dead.
                filterChain.doFilter(request, response);
                return;
            }
            String loginId = jwtService.extractUsername(token);
            if (loginId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(loginId);
                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            // Unknown/deleted user or malformed claims — continue unauthenticated
            // and let SecurityConfig produce the 401.
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
