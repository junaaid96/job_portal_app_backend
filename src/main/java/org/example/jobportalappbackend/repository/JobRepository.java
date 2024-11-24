package org.example.jobportalappbackend.repository;

import org.example.jobportalappbackend.model.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobPost, Integer> {
    List<JobPost> findByPostProfileContainingOrPostDescriptionContaining(
            String postProfile, String postDescription);
}