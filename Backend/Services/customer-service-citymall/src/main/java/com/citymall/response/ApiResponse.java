package com.citymall.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse<T> {
    private String message;
    private String statusCode;
    private boolean success;
    private T data;
    @Builder.Default
    private String timestamp = java.time.LocalDateTime.now().toString();
}
