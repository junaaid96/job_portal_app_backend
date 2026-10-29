package org.example.jobportalappbackend.dto;

/**
 * What {@code POST /register} returns. The password hash is intentionally absent.
 * The frontend only checks that the response is successful JSON, so dropping the hash does not change its flow.
 */
public record UserResponse(int userId, String username) {
}
