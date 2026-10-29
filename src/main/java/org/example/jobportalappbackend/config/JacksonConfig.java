package org.example.jobportalappbackend.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * Tweaks the Jackson 3 {@code JsonMapper} that Boot builds for HTTP JSON.
 *
 * <p>Boot 4 uses Jackson 3 ({@code tools.jackson}), not the Jackson 2 packages ({@code com.fasterxml.jackson.databind}).
 * Annotations such as {@code @JsonIgnore} still live in {@code com.fasterxml.jackson.annotation}.
 */
@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer frontendCompatibilityCustomizer() {
        return builder -> builder
                // A client may send a field we do not model yet. Ignore it instead of failing the whole request.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // The React form stores <input type="number"> as a string, so JSON often contains
                // "requiredExperience": "2". Accept that string and convert it to an integer.
                // This is limited to integer logical types; other coercions stay strict.
                .withCoercionConfig(LogicalType.Integer, config ->
                        config.setCoercion(CoercionInputShape.String, CoercionAction.TryConvert));
    }
}
