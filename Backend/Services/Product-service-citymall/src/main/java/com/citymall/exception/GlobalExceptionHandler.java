package com.citymall.exception;

import com.citymall.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductPurchaseException.class)
    public ResponseEntity<ApiResponse<?>> handle(ProductPurchaseException ex) {
        ApiResponse<?> response = ApiResponse.builder()
                .message(ex.getMessage())
                .success(false)
                .data(null)
                .build();
        return ResponseEntity.status(409).body(response);
    }

    @ExceptionHandler(ResourceNotFound.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFound ex) {
        ApiResponse<?> response = ApiResponse.builder()
                .message(ex.getMessage())
                .success(false)
                .data(null)
                .build();

        return ResponseEntity.status(404).body(response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestException ex) {
        ApiResponse<?> response = ApiResponse.builder()
                .message(ex.getMessage())
                .success(false)
                .data(null)
                .build();

        return ResponseEntity.status(400).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex) {
        ApiResponse<?> response = ApiResponse.builder()
                .message(ex.getMessage())
                .success(false)
                .data(null)
                .build();

        return ResponseEntity.status(409).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthentication(AuthenticationException ex) {
        ApiResponse<?> response = ApiResponse.builder()
                .message("Authentication failed: " + ex.getMessage())
                .success(false)
                .data(null)
                .build();
        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        ApiResponse<?> response = ApiResponse.builder()
                .message("Access denied: " + ex.getMessage())
                .success(false)
                .data(null)
                .build();
        return ResponseEntity.status(403).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex) {
        ApiResponse<?> response = ApiResponse.builder()
                .message("Something went wrong Kuch Fata hai bhai:  " + ex.getMessage())
                .success(false)
                .data(null)
                .build();
        return ResponseEntity.status(500).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(ex.getMessage());

        ApiResponse<?> response = ApiResponse.builder()
                .message(errorMessage)
                .success(false)
                .data(null)
                .build();

        return ResponseEntity.status(400).body(response);
    }
}