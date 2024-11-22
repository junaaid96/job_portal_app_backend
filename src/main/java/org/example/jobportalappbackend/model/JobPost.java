package org.example.jobportalappbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
@Entity
public class JobPost {
    @Id
    private int postId;
    private String postProfile;
    private String postDescription;
    private int requiredExperience;
//    @ElementCollection
//    @CollectionTable(name = "job_tech_stack", joinColumns = @JoinColumn(name = "post_id"))
//    @Column(name = "tech_stack")
    private List<String> postTechStack;
}