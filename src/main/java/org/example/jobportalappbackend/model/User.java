package org.example.jobportalappbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A registered account. This is a persistence object: it matches the {@code users} table and is not returned
 * from the API. The HTTP shape is {@code UserResponse}, which has no password field.
 *
 * <p>The class is not a record. JPA needs a mutable object and a no-argument constructor so Hibernate can
 * build a row and then fill the fields. Lombok {@code @Data} used to generate equals/hashCode from every
 * column, which is a poor fit for a persisted entity, so the accessors are written out.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int userId;

    /**
     * Unique login name. {@code nullable = false} is a database constraint, not just a Java check.
     * The column name stays {@code user_name} so databases created by the previous version still line up.
     */
    @Column(name = "user_name", nullable = false, unique = true)
    private String username;

    /**
     * BCrypt hash, never the password the user typed. {@code @JsonIgnore} is a second line of defense:
     * if a future change accidentally serializes this entity, the hash is left out of the JSON.
     */
    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Hibernate calls this, then sets the fields. Application code should use the setters.
     */
    public User() {
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Omits the password hash so a log statement cannot leak it.
     */
    @Override
    public String toString() {
        return "User{userId=" + userId + ", username='" + username + "'}";
    }
}
