package org.example.jobportalappbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.example.jobportalappbackend.config.OpenApiConfig;
import org.example.jobportalappbackend.dto.JobResponse;
import org.example.jobportalappbackend.dto.JobWriteRequest;
import org.example.jobportalappbackend.security.UserPrincipal;
import org.example.jobportalappbackend.service.JobService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Job routes. URLs are unchanged from the previous app so the Next.js client can keep calling them.
 *
 * <p>{@code @Validated} turns constraint annotations on method parameters ({@code @Positive}) into
 * {@code ConstraintViolationException}. {@code @Valid} on a body is separate: it checks the record's fields.
 *
 * <p>{@code @AuthenticationPrincipal} is the {@link UserPrincipal} the JWT filter stored. These methods do not
 * parse the Authorization header themselves.
 */
@RestController
@Validated
@Tag(name = "Jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @Operation(summary = "List every job post")
    @GetMapping(value = "/jobPosts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JobResponse> getJobPosts() {
        return jobService.getJobPosts();
    }

    @Operation(summary = "Get one job post")
    @GetMapping(value = "/jobPost/{postId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public JobResponse getJobPost(@PathVariable("postId") @Positive(message = "postId must be positive") int postId) {
        return jobService.getJobPost(postId);
    }

    /**
     * Returns 200, not 201. The frontend treats any status other than 200 as a failed create
     * ({@code response.status !== 200}), so 201 would break "Add New Job" even though the row was saved.
     */
    @Operation(summary = "Create a job post")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PostMapping(value = "/jobPost", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JobResponse addJob(@Valid @RequestBody JobWriteRequest request,
                              @AuthenticationPrincipal UserPrincipal principal) {
        return jobService.addJob(request, principal.getUsername());
    }

    @Operation(summary = "Update a job post you created")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PutMapping(value = "/jobPost", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JobResponse updateJob(@Valid @RequestBody JobWriteRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return jobService.updateJob(request, principal.getUsername());
    }

    /**
     * The success body is this exact sentence. The previous endpoint returned it as plain text, and it is kept
     * so a client that displays the body does not see a new message. Failures are Problem Details, not this string.
     */
    @Operation(summary = "Delete a job post you created")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @DeleteMapping(value = "/jobPost/{postId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteJob(@PathVariable("postId") @Positive(message = "postId must be positive") int postId,
                            @AuthenticationPrincipal UserPrincipal principal) {
        jobService.deleteJob(postId, principal.getUsername());
        return "This Job Post is Deleted!";
    }

    @Operation(summary = "Search job titles and descriptions")
    @GetMapping(value = "/jobPosts/search/{keyword}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JobResponse> search(@PathVariable("keyword") String keyword) {
        return jobService.search(keyword);
    }
}
