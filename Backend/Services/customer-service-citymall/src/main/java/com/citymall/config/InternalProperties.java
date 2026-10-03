package com.citymall.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds {@code app.internal.*}. Service-to-service calls (order-service Feign)
 * send this as {@code X-Internal-Secret}. Blank in dev = permitted without secret.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.internal")
public class InternalProperties {

    private String secret = "";
}
