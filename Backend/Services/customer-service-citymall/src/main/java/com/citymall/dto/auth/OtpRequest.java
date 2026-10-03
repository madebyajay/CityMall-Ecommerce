package com.citymall.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record OtpRequest(
        /** Email, mobile number or login id the OTP is issued for. */
        @NotBlank String identifier
) {
}
