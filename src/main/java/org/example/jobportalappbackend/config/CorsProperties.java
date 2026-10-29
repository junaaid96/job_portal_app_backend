package org.example.jobportalappbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds {@code app.cors.allowed-origins}. A comma-separated property becomes this list.
 * The frontend origin has to be listed explicitly because credentialed CORS cannot use {@code *}.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("app.cors.allowed-origins must list at least one origin");
        }
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}
