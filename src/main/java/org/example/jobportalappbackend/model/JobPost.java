package org.example.jobportalappbackend.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * One job posting, stored in {@code job_posts}. The tech stack is not columns on this row; it is a separate
 * collection table ({@code job_post_tech_stack}) because a job has many technology names and they are plain strings,
 * not their own entity.
 *
 * <p>Like {@link User}, this stays a class rather than a record so Hibernate can construct and change it.
 */
@Entity
@Table(name = "job_posts")
public class JobPost {

    /**
     * {@code IDENTITY} asks PostgreSQL for a new id on insert (a serial / identity column).
     * That matches the table the previous app already created. A sequence generator would be a different schema.
     */
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

    /**
     * {@code @ElementCollection} means "a collection of values that belong to this entity", not a relationship
     * to another aggregate. {@code LAZY} is the default: the list is loaded when something reads it inside a
     * transaction. Repositories that return jobs to the service use an entity graph so that happens in one query.
     */
    @ElementCollection
    @CollectionTable(name = "job_post_tech_stack", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "technology")
    private List<String> postTechStack = new ArrayList<>();

    /** Login name of the account that created the job. The service sets this from the JWT, not from the request body. */
    @Column(name = "added_by", length = 100)
    private String addedBy;

    public JobPost() {
    }

    public int getPostId() {
        return postId;
    }

    public String getPostProfile() {
        return postProfile;
    }

    public void setPostProfile(String postProfile) {
        this.postProfile = postProfile;
    }

    public String getPostDescription() {
        return postDescription;
    }

    public void setPostDescription(String postDescription) {
        this.postDescription = postDescription;
    }

    public int getRequiredExperience() {
        return requiredExperience;
    }

    public void setRequiredExperience(int requiredExperience) {
        this.requiredExperience = requiredExperience;
    }

    public List<String> getPostTechStack() {
        return postTechStack;
    }

    public void setPostTechStack(List<String> postTechStack) {
        this.postTechStack = postTechStack;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    @Override
    public String toString() {
        return "JobPost{postId=" + postId + ", postProfile='" + postProfile + "', addedBy='" + addedBy + "'}";
    }
}
