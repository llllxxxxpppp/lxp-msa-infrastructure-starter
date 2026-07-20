package com.lcs.auth.principal;

import java.io.Serial;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserPrincipal implements UserDetails {
    @Serial
    private static final long serialVersionUID = 3160057928138423721L;

    private final Long userId;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean isDeleted;

    public CustomUserPrincipal(
            Long userId,
            String email,
            String password,
            Collection<? extends GrantedAuthority> authorities,
            boolean isDeleted
    ) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.isDeleted = isDeleted;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return !isDeleted;
    }
}
