package org.example.jobportalappbackend.controller;

import org.example.jobportalappbackend.model.JobPost;
import org.example.jobportalappbackend.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@Controller // @Controller is used to define a controller and has methods to return response in view or data type. if you use @Controller, you need to use @ResponseBody to return data in the response body. otherwise, it will try to find a view with the name returned by the method.
@CrossOrigin(origins = "http://localhost:3000") // @CrossOrigin is used to handle the request from a different origin.
public class HomeController {
    @Autowired
    private JobService jobService;

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
    public JobPost updateJob(@RequestBody JobPost jobPost) {
        jobService.updateJob(jobPost);
        return jobService.getJobPost(jobPost.getPostId());
    }

    @DeleteMapping("/jobPost/{postId}")
    public String deleteJob(@PathVariable int postId) {
        JobPost jobPost = jobService.getJobPost(postId);
        if (jobPost == null) {
            return "This Job Post does not exist!";
        }
        jobService.deleteJob(postId);
        return "This Job Post is Deleted!";
    }

    @GetMapping("/loadData")
    public String loadData() {
        jobService.loadData();
        return "Data Loaded!";
    }

    @GetMapping("/jobPosts/search/{keyword}")
    public List<JobPost> searchJobPosts(@PathVariable String keyword) {
        return jobService.searchJobPosts(keyword);
    }
}

