package com.libraryms.common.security;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.libraryms.user.RoleName;

public record CurrentUserPrincipal(
        Long id,
        String username,
        String email,
        Long memberId,
        Set<RoleName> roles,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails, Principal, Serializable {

    @Override
    public String getName() {
        return username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // Not exposed or held in principal
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

    public boolean hasRole(RoleName roleName) {
        return roles != null && roles.contains(roleName);
    }
}
