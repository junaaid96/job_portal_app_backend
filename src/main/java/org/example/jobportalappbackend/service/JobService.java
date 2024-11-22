package org.example.jobportalappbackend.service;

import org.example.jobportalappbackend.model.JobPost;
import org.example.jobportalappbackend.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class JobService {
    @Autowired
    private JobRepository jobRepository;

    // DTO: jobPost (transfer between Controller to Service to Repository)
    public void addJob(JobPost jobPost) {
        jobRepository.save(jobPost);
    }

    public List<JobPost> getJobPosts() {
        return jobRepository.findAll();
    }

    public JobPost getJobPost(int postId) {
        return jobRepository.findById(postId).orElse(new JobPost());
    }

    public void updateJob(JobPost jobPost) {
        jobRepository.save(jobPost);
    }

    public void deleteJob(int postId) {
        jobRepository.deleteById(postId);
    }

    public void loadData() {
        List<JobPost> jobPosts = new ArrayList<>(Arrays.asList(
                new JobPost(1, "Java Developer", "We are looking for a skilled Java Developer with experience in Spring and Hibernate frameworks. The ideal candidate will have a strong understanding of Java programming and be able to work on complex projects.", 2, Arrays.asList("Java", "Spring", "Hibernate")),
                new JobPost(2, "Python Developer", "Join our team as a Python Developer. You should have experience with Django and Flask frameworks. The role involves developing and maintaining web applications using Python.", 2, Arrays.asList("Python", "Django", "Flask")),
                new JobPost(3, "JavaScript Developer", "We need a JavaScript Developer proficient in React and Angular. The candidate will be responsible for building dynamic and responsive web applications using JavaScript.", 2, Arrays.asList("JavaScript", "React", "Angular")),
                new JobPost(4, "Rust Developer", "We are looking for a skilled Rust Developer with experience in Actix and Rocket frameworks. The ideal candidate will have a strong understanding of Rust programming and be able to work on complex projects.", 2, Arrays.asList("Rust", "Actix", "Rocket")),
                new JobPost(5, "Go Developer", "Join our team as a Go Developer. You should have experience with Gin and Echo frameworks. The role involves developing and maintaining web applications using Go.", 2, Arrays.asList("Go", "Gin", "Echo"))));

        jobRepository.saveAll(jobPosts);
    }

    public List<JobPost> searchJobPosts(String keyword) {
        return jobRepository.findByPostProfileContainingOrPostDescriptionContaining(keyword, keyword);
    }
}