package org.example.jobportalappbackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_posts")
public class JobPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private int postId;

    @Column(name = "post_profile", length = 100)
    private String postProfile;

    @Column(name = "post_description", columnDefinition = "TEXT")
    private String postDescription;

    @Column(name = "required_experience")
    private int requiredExperience;

    @ElementCollection
    @CollectionTable(
        name = "job_post_tech_stack",
        joinColumns = @JoinColumn(name = "post_id")
    )
    @Column(name = "technology")
    private List<String> postTechStack;
}