package org.example.jobportalappbackend.mapper;

import org.example.jobportalappbackend.dto.JobResponse;
import org.example.jobportalappbackend.model.JobPost;

import java.util.List;

/**
 * Copies an entity into the HTTP record. This is a pure function, so it is not a Spring bean:
 * it has no dependencies and no reason to be injected.
 *
 * <p>Call it inside the transaction that loaded {@code job}. Reading {@code postTechStack} is what initializes
 * the collection. {@link List#copyOf} detaches the response from Hibernate's persistent list.
 */
public final class JobMapper {

    private JobMapper() {
    }

    public static JobResponse toResponse(JobPost job) {
        List<String> technologies = job.getPostTechStack() == null
                ? List.of()
                : List.copyOf(job.getPostTechStack());
        return new JobResponse(
                job.getPostId(),
                job.getPostProfile(),
                job.getPostDescription(),
                job.getRequiredExperience(),
                technologies,
                job.getAddedBy());
    }
}
