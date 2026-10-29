package org.example.jobportalappbackend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /login}: {@code {"username":"...","password":"..."}}.
 * A blank value is rejected. The controller turns that into the same plain-text 401 the frontend already checks,
 * rather than a JSON problem body (see {@code GlobalExceptionHandler}).
 */
public record LoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
) {
}
