package org.example.jobportalappbackend.repository;

import org.example.jobportalappbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data implements this interface at startup. The method names are the query.
 * {@code findByUsername} becomes {@code SELECT ... FROM users WHERE user_name = ?}.
 *
 * <p>{@code @Repository} is optional on a Spring Data interface. Extending {@link JpaRepository} is already enough
 * for Boot to detect it. {@code Integer} is the type of {@link User}'s id.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
