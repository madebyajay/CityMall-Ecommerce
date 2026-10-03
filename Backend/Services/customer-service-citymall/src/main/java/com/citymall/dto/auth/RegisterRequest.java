package com.citymall.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Login id is required")
        @Size(min = 4, max = 100, message = "Login id must be 4-100 characters")
        String userLoginId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        // At least 1 letter + 1 digit; adjust policy in one place
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
        Long mobileNumber,

        /** Optional. Defaults to CUSTOMER when blank. */
        String roleName
) {
}
