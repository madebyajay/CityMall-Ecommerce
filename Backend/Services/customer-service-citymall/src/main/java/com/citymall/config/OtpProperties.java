package com.citymall.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Binds {@code app.otp.*} — OTP generation limits (OTP:{key} in Redis). */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.otp")
public class OtpProperties {

    private int length = 6;

    /** Max OTP generations per key before the window expires. */
    private int limit = 5;

    private long expiryMinutes = 5L;
}
