package com.citymall.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /api/v1/admin/create-admin}.
 * Only callable with an existing ADMIN JWT (see SecurityConfig).
 * Mirrors {@link com.citymall.dto.auth.RegisterRequest} validation style.
 */
public record CreateAdminRequest(

        @NotBlank(message = "Login id is required")
        @Size(min = 4, max = 100, message = "Login id must be 4-100 characters")
        String userLoginId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must contain at least one letter and one number")
        String password,

        @NotBlank(message = "User name is required")
        String userName,

        String firstName,

        String lastName,

        @Email(message = "Email should be valid")
        String emailId,

        @NotNull(message = "Mobile number is required")
        @Min(value = 1000000000L, message = "Mobile number must be 10 digits")
        Long mobileNumber
) {
}
