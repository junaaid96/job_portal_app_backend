package org.example.jobportalappbackend.controller;

import org.example.jobportalappbackend.dto.LoginRequest;
import org.example.jobportalappbackend.dto.RegisterRequest;
import org.example.jobportalappbackend.dto.UserResponse;
import org.example.jobportalappbackend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * {@code /login} and {@code /register}. Paths stay at the server root because the frontend calls them there.
 * There is no {@code /api} prefix on purpose.
 */
@RestController
@Tag(name = "Accounts")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@code produces = text/plain} so the body is the JWT characters, not a JSON string with extra quotes.
     * {@code @Valid} runs the constraints on {@link LoginRequest} before this method is called.
     */
    @Operation(summary = "Log in and receive a raw JWT")
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    /**
     * 201 Created. The frontend treats any 2xx response as success. The body has {@code userId} and {@code username} only.
     */
    @Operation(summary = "Create an account")
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }
}
