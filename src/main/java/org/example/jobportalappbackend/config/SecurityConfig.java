package org.example.jobportalappbackend.config;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import org.example.jobportalappbackend.security.JobUserDetailsService;
import org.example.jobportalappbackend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTP security. This is the lambda DSL ({@code http.csrf(customizer)}), not the removed
 * {@code WebSecurityConfigurerAdapter}.
 *
 * <p>Constructor injection supplies the JWT filter. The class does not call {@code @Autowired} on fields:
 * the dependencies are visible, final, and easier to replace in a test.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonMapper jsonMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, JsonMapper jsonMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jsonMapper = jsonMapper;
    }

    /**
     * BCrypt with cost 12, the same cost the previous app used when it hashed passwords.
     * {@code matches} reads the cost from the stored hash, so existing rows still verify.
     * One bean is shared by registration and by login. The old code constructed two separate encoders.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Used only by {@code POST /login}. The JWT filter does not go through this manager; it sets the
     * {@code SecurityContext} itself after the signature check.
     *
     * <p>Spring Security 7 removed the no-arg {@code DaoAuthenticationProvider} constructor.
     * The provider now takes the {@link JobUserDetailsService} up front. {@link ProviderManager} is the
     * {@link AuthenticationManager} implementation that delegates to that provider.
     */
    @Bean
    AuthenticationManager authenticationManager(JobUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * The filter chain, in the order the lambdas configure it:
     * <ol>
     *   <li>CSRF is off. CSRF tokens protect cookie sessions. This API is stateless and uses an Authorization header.</li>
     *   <li>CORS uses the rules from {@link WebConfig}.</li>
     *   <li>Public GETs, login, register, OpenAPI, and health do not need a token. Every other route does.</li>
     *   <li>No HTTP session is created. Each request must carry its own JWT.</li>
     *   <li>Form login and HTTP Basic are off so a browser popup or a session form cannot appear.</li>
     *   <li>Missing credentials and forbidden access are written as Problem Details.</li>
     *   <li>The JWT filter runs before the username/password filter (which is not doing form login, but is the
     *       standard insertion point for a bearer token).</li>
     * </ol>
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, "/jobPosts", "/jobPost/**", "/jobPosts/search/**").permitAll()
                        .requestMatchers("/login", "/register", "/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeProblem(response, HttpStatus.UNAUTHORIZED, "Authentication is required"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeProblem(response, HttpStatus.FORBIDDEN, "You do not have access to this resource")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Security's entry points are servlet callbacks, not controller methods, so they do not go through
     * {@code GlobalExceptionHandler}. This writes the same Problem Detail shape by hand.
     * {@link JsonMapper} is the Jackson 3 mapper Boot configures for HTTP JSON.
     */
    private void writeProblem(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
