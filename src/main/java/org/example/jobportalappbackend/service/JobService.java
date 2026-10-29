package org.example.jobportalappbackend.service;

import org.example.jobportalappbackend.dto.JobResponse;
import org.example.jobportalappbackend.dto.JobWriteRequest;
import org.example.jobportalappbackend.exception.ForbiddenJobAccessException;
import org.example.jobportalappbackend.exception.JobNotFoundException;
import org.example.jobportalappbackend.mapper.JobMapper;
import org.example.jobportalappbackend.model.JobPost;
import org.example.jobportalappbackend.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Job use cases. Class-level {@code @Transactional(readOnly = true)} wraps every public method in a read-only
 * transaction. Write methods declare {@code @Transactional} again, which replaces the class setting for that
 * method (annotations are not merged) and opens a read-write transaction.
 *
 * <p>The owner of a job is {@code username} from the security principal. The {@code addedBy} field in the body
 * is ignored. The previous code compared that body field to the token, so any logged-in user could overwrite
 * someone else's job by sending their own name as {@code addedBy}.
 */
@Service
@Transactional(readOnly = true)
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public List<JobResponse> getJobPosts() {
        return jobRepository.findAll().stream().map(JobMapper::toResponse).toList();
    }

    public JobResponse getJobPost(int postId) {
        return jobRepository.findWithTechStackByPostId(postId)
                .map(JobMapper::toResponse)
                .orElseThrow(() -> new JobNotFoundException(postId));
    }

    public List<JobResponse> search(String keyword) {
        return jobRepository.searchByKeyword(keyword).stream().map(JobMapper::toResponse).toList();
    }

    @Transactional
    public JobResponse addJob(JobWriteRequest request, String username) {
        JobPost job = new JobPost();
        copyEditableFields(job, request);
        job.setAddedBy(username);
        return JobMapper.toResponse(jobRepository.save(job));
    }

    @Transactional
    public JobResponse updateJob(JobWriteRequest request, String username) {
        if (request.postId() == null || request.postId() < 1) {
            throw new IllegalArgumentException("postId is required to update a job");
        }
        JobPost existing = jobRepository.findWithTechStackByPostId(request.postId())
                .orElseThrow(() -> new JobNotFoundException(request.postId()));
        requireOwner(existing, username, "You are not authorized to edit this job post");
        copyEditableFields(existing, request);
        // addedBy stays as it was stored. A client cannot reassign the job by editing the body.
        return JobMapper.toResponse(jobRepository.save(existing));
    }

    @Transactional
    public void deleteJob(int postId, String username) {
        JobPost existing = jobRepository.findWithTechStackByPostId(postId)
                .orElseThrow(() -> new JobNotFoundException(postId));
        requireOwner(existing, username, "You are not authorized to delete this job post");
        jobRepository.delete(existing);
    }

    /**
     * Mutates the persistent collection in place. Replacing the list with a brand-new instance sometimes
     * leaves the old {@code job_post_tech_stack} rows behind, because Hibernate is tracking the collection
     * object it loaded. Clearing and adding tells it exactly which elements changed.
     */
    private void copyEditableFields(JobPost job, JobWriteRequest request) {
        job.setPostProfile(request.postProfile());
        job.setPostDescription(request.postDescription());
        job.setRequiredExperience(request.requiredExperience());
        if (job.getPostTechStack() == null) {
            job.setPostTechStack(new ArrayList<>());
        }
        job.getPostTechStack().clear();
        job.getPostTechStack().addAll(request.postTechStack());
    }

    private void requireOwner(JobPost job, String username, String message) {
        if (!username.equals(job.getAddedBy())) {
            throw new ForbiddenJobAccessException(message);
        }
    }
}
