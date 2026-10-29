package org.example.jobportalappbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per HTTP request, before Spring Security decides whether the route is allowed.
 *
 * <p>{@link OncePerRequestFilter} skips the extra dispatcher hops (error, async) that a raw servlet filter would see.
 * A missing or bad token does not abort the request: public routes such as {@code GET /jobPosts} still work.
 * Protected routes then fail later with 401 because no authentication was stored.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JobUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, JobUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Pattern switch: null and any non-Bearer header take the same path (stay anonymous).
        switch (request.getHeader("Authorization")) {
            case String header when header.startsWith("Bearer ") -> authenticate(header.substring("Bearer ".length()), request);
            case null, default -> {
                // No credentials. Public routes continue; protected routes are rejected by the security chain.
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        jwtService.extractValidUsername(token).ifPresent(username -> {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UsernameNotFoundException ex) {
                SecurityContextHolder.clearContext();
            }
        });
    }
}
