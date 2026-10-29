package org.example.jobportalappbackend.security;

import org.example.jobportalappbackend.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The object Spring Security stores after a successful login or a valid JWT.
 * It copies the username and password hash out of {@link User} so the security layer does not keep a Hibernate
 * entity (and its session) around for the rest of the request.
 *
 * <p>The class name replaces the old {@code UserPrinciple} spelling. Nothing outside this process depended on that name.
 */
public final class UserPrincipal implements UserDetails {

    private final String username;
    private final String password;

    public UserPrincipal(User user) {
        this.username = user.getUsername();
        this.password = user.getPassword();
    }

    /**
     * Every account currently has the same authority. {@code hasRole("USER")} looks for {@code ROLE_USER};
     * the prefix is Spring Security's convention. The HTTP rules in this app use {@code authenticated()},
     * so the role is not checked yet. It is here so a later rule can use it.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /** Account flags we do not store. Returning true means "this account is allowed to log in". */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
