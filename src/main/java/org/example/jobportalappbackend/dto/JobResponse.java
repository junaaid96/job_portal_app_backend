package org.example.jobportalappbackend.dto;

import java.util.List;

/**
 * JSON body for a job. The component names are the JSON property names the frontend already reads:
 * {@code postId}, {@code postProfile}, {@code postDescription}, {@code requiredExperience},
 * {@code postTechStack}, {@code addedBy}.
 *
 * <p>A record gives us a constructor, accessors, {@code equals}, and {@code hashCode} without a setter.
 * The response cannot be changed after it is built, which is what we want for an HTTP payload.
 */
public record JobResponse(
        int postId,
        String postProfile,
        String postDescription,
        int requiredExperience,
        List<String> postTechStack,
        String addedBy
) {
}
