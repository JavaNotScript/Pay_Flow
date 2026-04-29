package com.payflow.auth.internal.security;

import com.payflow.common.security.CurrentUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AuthenticatedUser implements UserDetails, CurrentUser {
    private final Long userId;
    private final String username;
    private final String role;
    private final String password;

    //used in JWTFilter
    public AuthenticatedUser(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;

        password = null;
    }

    //Used during authentication
    public AuthenticatedUser(Long userId, String username, String role, String password) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

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

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public String getEmail() {
        return username;
    }

    @Override
    public String getRole() {
        return role;
    }
}
