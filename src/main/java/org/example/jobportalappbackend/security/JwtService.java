package org.example.jobportalappbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.jobportalappbackend.config.JwtProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Creates and reads HS256 JWTs. The token is a signed string {@code header.payload.signature}.
 * The payload's {@code sub} claim is the username. The frontend reads that claim to greet the user.
 *
 * <p>jjwt 0.12+ removed {@code parserBuilder()}, {@code setSubject}, and {@code SignatureAlgorithm} on the builder.
 * {@code signWith(SecretKey)} picks HMAC-SHA256 when the key is at least 256 bits and shorter than 384 bits.
 * {@code javax.crypto.SecretKey} is the JDK cryptography type, not the old Java EE {@code javax.servlet} package.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expiration())))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and the expiry, then returns {@code sub}.
     * An empty optional means "not a token we trust"; the filter then leaves the request anonymous.
     * The token itself is never logged.
     */
    public Optional<String> extractValidUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Date expiration = claims.getExpiration();
            String subject = claims.getSubject();
            if (expiration == null || !expiration.after(new Date()) || subject == null || subject.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(subject);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
