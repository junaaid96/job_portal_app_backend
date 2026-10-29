package org.example.jobportalappbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /register}. BCrypt only uses the first 72 bytes of a password, so longer values are rejected
 * here instead of being silently truncated.
 */
public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 100, message = "Username must be at most 100 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password must be at most 72 characters")
        String password
) {
}
