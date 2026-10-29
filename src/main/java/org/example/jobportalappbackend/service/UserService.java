package org.example.jobportalappbackend.service;

import org.example.jobportalappbackend.dto.LoginRequest;
import org.example.jobportalappbackend.dto.RegisterRequest;
import org.example.jobportalappbackend.dto.UserResponse;
import org.example.jobportalappbackend.exception.DuplicateUsernameException;
import org.example.jobportalappbackend.model.User;
import org.example.jobportalappbackend.repository.UserRepository;
import org.example.jobportalappbackend.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account use cases. The controller does not hash passwords or call the authentication manager itself.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Hashes the password, stores the user, and returns the public fields only.
     * {@code saveAndFlush} sends the insert immediately so a unique-constraint race becomes an exception
     * we can translate, instead of waiting until the transaction commits outside this method.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException("Username is already taken");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        try {
            User saved = userRepository.saveAndFlush(user);
            return new UserResponse(saved.getUserId(), saved.getUsername());
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateUsernameException("Username is already taken");
        }
    }

    /**
     * Checks the password hash. {@code authenticate} throws {@code BadCredentialsException} when the
     * username or password is wrong. There is no {@code else} branch: the old one never ran, because a
     * failed login already threw before the {@code if}.
     *
     * @return the raw JWT string, not a JSON object. The frontend stores {@code response.text()} as the token.
     */
    public String login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        return jwtService.generateToken(authentication.getName());
    }
}
