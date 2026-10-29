package org.example.jobportalappbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of {@code POST /jobPost} and {@code PUT /jobPost}.
 *
 * <p>{@code postId} is ignored on create (the database assigns it) and required on update.
 * {@code addedBy} is accepted so the existing frontend payload still validates, but the service
 * does not trust it: the owner is always the authenticated user.
 *
 * <p>Constraint annotations are checked when the controller parameter is marked {@code @Valid}.
 * A failure becomes HTTP 400 with a Problem Detail, not a saved row.
 */
public record JobWriteRequest(
        Integer postId,

        @NotBlank(message = "Job title is required")
        @Size(max = 100, message = "Job title must be at most 100 characters")
        String postProfile,

        @NotBlank(message = "Job description is required")
        String postDescription,

        @NotNull(message = "Required experience is required")
        @Min(value = 0, message = "Required experience cannot be negative")
        Integer requiredExperience,

        @NotNull(message = "postTechStack is required")
        List<@NotBlank(message = "A technology name cannot be blank") @Size(max = 100) String> postTechStack,

        String addedBy
) {
}
