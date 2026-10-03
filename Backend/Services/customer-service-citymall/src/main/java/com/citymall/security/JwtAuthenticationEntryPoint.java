package com.citymall.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication entry point for handling unauthorized access
 * attempts.
 * <p>
 * This class implements {@link AuthenticationEntryPoint} and provides a
 * JSON-based response when an unauthorized request is made.
 * </p>
 * @author Ajay.Gour
 */

@Component
public class JwtAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    /**
     * Handles unauthorized access by returning a JSON response with error
     * details.
     * @param request       the HTTP request
     * @param response      the HTTP response
     * @param authException the exception triggered due to unauthorized
     *                      access
     * @throws IOException      if an input or output exception occurs
     * @throws ServletException if a servlet-related exception occurs
     */
    @Override
    public void commence(final HttpServletRequest request,
                         final HttpServletResponse response,
                         final AuthenticationException authException)
            throws IOException, ServletException {

        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("message",
                "Access Denied !! " + authException.getMessage());
        errorInfo.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorInfo.put("data", null);
        errorInfo.put("error", "true");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        // Convert the response to JSON and write it to the response body
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(errorInfo);

        response.setContentType("application/json");
        response.getWriter().write(jsonResponse);
    }
}

