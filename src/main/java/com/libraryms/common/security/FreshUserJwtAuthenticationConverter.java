package com.libraryms.common.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.libraryms.user.entity.RoleName;
import com.libraryms.user.entity.User;
import com.libraryms.user.repository.UserRepository;

/**
 * Validates JWT access tokens with a fresh database lookup on each request.
 * Deleted, disabled, or demoted users lose access immediately.
 */
@Component
public class FreshUserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public FreshUserJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String subject = jwt.getSubject();
        Long userId;
        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Invalid token subject: must be a numeric user ID");
        }

        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new BadCredentialsException("User account does not exist or has been removed"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("User account has been disabled");
        }

        Set<RoleName> roleNames = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());

        Set<SimpleGrantedAuthority> authorities = roleNames.stream()
                .map(r -> new SimpleGrantedAuthority(r.name()))
                .collect(Collectors.toSet());

        Long memberId = user.getMember() != null ? user.getMember().getId() : null;

        CurrentUserPrincipal principal = new CurrentUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                memberId,
                roleNames,
                authorities
        );

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }
}
