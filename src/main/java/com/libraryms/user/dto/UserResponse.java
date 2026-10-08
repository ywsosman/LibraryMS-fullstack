package com.libraryms.user.dto;

import com.libraryms.user.entity.User;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        boolean enabled,
        Long memberId,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {}
