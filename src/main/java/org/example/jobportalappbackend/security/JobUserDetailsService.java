package org.example.jobportalappbackend.security;

import org.example.jobportalappbackend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * How Spring Security loads an account. The method name is fixed by {@link UserDetailsService}:
 * {@code loadUserByUsername} is called during {@code POST /login} and again when a JWT is presented.
 *
 * <p>{@code @Transactional(readOnly = true)} opens a short transaction for the select and tells Hibernate
 * it does not need to flush changes (there are none).
 */
@Service
public class JobUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public JobUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
