package org.example.jobportalappbackend.repository;

import org.example.jobportalappbackend.model.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobPost, Integer> {
    @Query("SELECT DISTINCT j FROM JobPost j WHERE " +
            "LOWER(j.postProfile) LIKE LOWER(concat('%', :postProfile, '%')) OR " +
            "LOWER(j.postDescription) LIKE LOWER(concat('%', :postDescription, '%'))")
    List<JobPost> findByPostProfileContainingOrPostDescriptionContaining(
            @Param("postProfile") String postProfile,
            @Param("postDescription") String postDescription
    );
}

//List<JobPost> jobPosts = new ArrayList<>(Arrays.asList(
//        new JobPost(1, "Java Developer", "We are looking for a skilled Java Developer with experience in Spring and Hibernate frameworks. The ideal candidate will have a strong understanding of Java programming and be able to work on complex projects.", 2, Arrays.asList("Java", "Spring", "Hibernate")),
//        new JobPost(2, "Python Developer", "Join our team as a Python Developer. You should have experience with Django and Flask frameworks. The role involves developing and maintaining web applications using Python.", 2, Arrays.asList("Python", "Django", "Flask")),
//        new JobPost(3, "JavaScript Developer", "We need a JavaScript Developer proficient in React and Angular. The candidate will be responsible for building dynamic and responsive web applications using JavaScript.", 2, Arrays.asList("JavaScript", "React", "Angular")),
//        new JobPost(4, "Rust Developer", "We are looking for a skilled Rust Developer with experience in Actix and Rocket frameworks. The ideal candidate will have a strong understanding of Rust programming and be able to work on complex projects.", 2, Arrays.asList("Rust", "Actix", "Rocket")),
//        new JobPost(5, "Go Developer", "Join our team as a Go Developer. You should have experience with Gin and Echo frameworks. The role involves developing and maintaining web applications using Go.", 2, Arrays.asList("Go", "Gin", "Echo"))));
//
//
//public List<JobPost> getJobPosts() {
//    return jobPosts;
//}
//
//public JobPost getJobPost(int postId) {
//    return jobPosts.stream()
//            .filter(t -> t.getPostId() == postId)
//            .findFirst()
//            .orElse(null);
//}
//
/// / DTO: jobPost (transfer between Controller to Service to Repository)
//public void addJob(JobPost jobPost) {
//    jobPosts.add(jobPost);
//}
//
//public void updateJob(JobPost jobPost) {
//    for (JobPost jobPost1 : jobPosts) {
//        if (jobPost1.getPostId() == jobPost.getPostId()) {
//            jobPost1.setPostProfile(jobPost.getPostProfile());
//            jobPost1.setPostDescription(jobPost.getPostDescription());
//            jobPost1.setRequiredExperience(jobPost.getRequiredExperience());
//            jobPost1.setPostTechStack(jobPost.getPostTechStack());
//            return;
//        }
//    }
//}
//
//public void deleteJob(int postId) {
//    jobPosts.removeIf(t -> t.getPostId() == postId);
//}