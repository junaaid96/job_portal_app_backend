package org.example.jobportalappbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Binds {@code app.jwt.*} from application.properties.
 *
 * <p>A record is a good fit: the values are immutable, and Spring Boot passes them to the canonical constructor
 * (constructor binding). The compact constructor rejects a secret that is too short to sign an HS256 JWT.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "app.jwt.secret must be at least 32 bytes so HMAC-SHA256 is allowed");
        }
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("app.jwt.expiration must be a positive duration, for example 30m");
        }
    }
}
