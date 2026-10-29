package org.example.jobportalappbackend.repository;

import org.example.jobportalappbackend.model.JobPost;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Job queries. {@code @EntityGraph} tells Hibernate to fetch {@code postTechStack} in the same select
 * (a join) instead of one extra query per job. That matters because {@code spring.jpa.open-in-view} is false:
 * after the transaction ends, a lazy list can no longer be loaded.
 */
public interface JobRepository extends JpaRepository<JobPost, Integer> {

    @Override
    @EntityGraph(attributePaths = "postTechStack")
    List<JobPost> findAll();

    @EntityGraph(attributePaths = "postTechStack")
    Optional<JobPost> findWithTechStackByPostId(int postId);

    /**
     * Case-sensitive substring match on the title or the description, same rule as the previous derived query
     * {@code findByPostProfileContainingOrPostDescriptionContaining}. PostgreSQL {@code LIKE} is case-sensitive.
     * {@code :keyword} is a bind parameter, so the text cannot change the SQL statement.
     *
     * <p>The text block is ordinary Java text; the line breaks are not sent to the database as syntax.
     * A keyword that itself contains {@code %} or {@code _} is treated as a LIKE pattern. That matches the old behavior.
     */
    @EntityGraph(attributePaths = "postTechStack")
    @Query("""
            SELECT job
            FROM JobPost job
            WHERE job.postProfile LIKE CONCAT('%', :keyword, '%')
               OR job.postDescription LIKE CONCAT('%', :keyword, '%')
            """)
    List<JobPost> searchByKeyword(@Param("keyword") String keyword);
}
