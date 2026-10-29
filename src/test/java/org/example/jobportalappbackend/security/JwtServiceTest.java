package org.example.jobportalappbackend.security;

import org.example.jobportalappbackend.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * No Spring context and no database. {@link JwtService} only needs the settings record, so these tests stay fast.
 */
class JwtServiceTest {

    private static final String SECRET = "dev-only-change-me-please-32b-min!!";

    @Test
    void tokenRoundTripsTheUsername() {
        JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(30)));

        String token = service.generateToken("ada");

        assertThat(token.chars().filter(character -> character == '.').count()).isEqualTo(2);
        assertThat(service.extractValidUsername(token)).contains("ada");
    }

    @Test
    void rejectsATokenSignedWithAnotherKey() {
        JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(30)));
        JwtService other = new JwtService(new JwtProperties(
                "another-dev-secret-key-must-be-32b!!", Duration.ofMinutes(30)));

        assertThat(other.extractValidUsername(service.generateToken("ada"))).isEmpty();
    }

    @Test
    void rejectsMalformedToken() {
        JwtService service = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(30)));

        assertThat(service.extractValidUsername("not-a-jwt")).isEmpty();
    }

    @Test
    void rejectsAShortSecret() {
        assertThatThrownBy(() -> new JwtProperties("too-short", Duration.ofMinutes(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }
}
