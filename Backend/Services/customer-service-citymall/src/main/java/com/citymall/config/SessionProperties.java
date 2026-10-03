package com.citymall.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Binds {@code app.session.*} — single-device login sessions in Redis. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.session")
public class SessionProperties {

    /** When true, a new login kicks the previous device (forcedLogin bypasses the guard). */
    private boolean singleSession = true;

    /** TTL for LOGIN:{userId} entries. */
    private long accessTtlMinutes = 240L;

    /** TTL for REFRESH:{userId} entries. */
    private long refreshTtlDays = 7L;
}
