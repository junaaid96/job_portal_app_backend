package org.example.jobportalappbackend.controller;

import org.example.jobportalappbackend.model.JobPost;
import org.example.jobportalappbackend.service.JobService;
import org.example.jobportalappbackend.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HomeController {
    @Autowired
    private JobService jobService;
    @Autowired
    private JwtService jwtService;

    @GetMapping("/jobPosts")
    public List<JobPost> getJobPosts() {
        return jobService.getJobPosts();
    }

    @GetMapping("/jobPost/{postId}")
    public JobPost getJobPost(@PathVariable("postId") int postId) {
        return jobService.getJobPost(postId);
    }

    @PostMapping("/jobPost")
    public JobPost addJob(@RequestBody JobPost jobPost) {
        jobService.addJob(jobPost);
        return jobService.getJobPost(jobPost.getPostId());
    }

    @PutMapping("/jobPost")
    public ResponseEntity<?> updateJob(@RequestBody JobPost jobPost, @RequestHeader("Authorization") String token) {
        String username = jwtService.extractUsername(token.substring(7));
        if (!jobPost.getAddedBy().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to edit this job post");
        }
        jobService.updateJob(jobPost);
        return ResponseEntity.ok(jobService.getJobPost(jobPost.getPostId()));
    }

    @DeleteMapping("/jobPost/{postId}")
    public ResponseEntity<?> deleteJob(@PathVariable int postId, @RequestHeader("Authorization") String token) {
        JobPost jobPost = jobService.getJobPost(postId);
        if (jobPost == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("This Job Post does not exist!");
        }
        String username = jwtService.extractUsername(token.substring(7));
        if (!jobPost.getAddedBy().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to delete this job post");
        }
        jobService.deleteJob(postId);
        return ResponseEntity.ok("This Job Post is Deleted!");
    }

//    @PutMapping("/jobPost")
//    public JobPost updateJob(@RequestBody JobPost jobPost) {
//        jobService.updateJob(jobPost);
//        return jobService.getJobPost(jobPost.getPostId());
//    }
//
//    @DeleteMapping("/jobPost/{postId}")
//    public String deleteJob(@PathVariable int postId) {
//        JobPost jobPost = jobService.getJobPost(postId);
//        if (jobPost == null) {
//            return "This Job Post does not exist!";
//        }
//        jobService.deleteJob(postId);
//        return "This Job Post is Deleted!";
//    }

//    @GetMapping("/loadData")
//    public String loadData() {
//        jobService.loadData();
//        return "Data Loaded!";
//    }

    @GetMapping("/jobPosts/search/{keyword}")
    public List<JobPost> searchJobPosts(@PathVariable String keyword) {
        return jobService.searchJobPosts(keyword);
    }
}

