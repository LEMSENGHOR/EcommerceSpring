package com.ecommerce.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Base64-encoded secret. Must be at least 256 bits (32 bytes) once decoded
     * for HS256. Set via the APP_JWT_SECRET env var in real deployments —
     * never commit a real secret.
     */
    private String secret;

    /** Access token lifetime in milliseconds. Default: 15 minutes. */
    private long accessTokenExpirationMs = 900_000;

    /** Refresh token lifetime in milliseconds. Default: 7 days. */
    private long refreshTokenExpirationMs = 604_800_000;
}
