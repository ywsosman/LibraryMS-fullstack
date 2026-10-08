package com.libraryms.member.dto;

import com.libraryms.member.entity.Member;

import java.time.Instant;

public record MemberResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        Instant createdAt,
        Instant updatedAt
) {}
