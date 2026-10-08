package com.libraryms.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-service library card activation. The email is deliberately absent:
 * it is always taken from the signed-in user's account.
 */
public record ActivateMembershipRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 200, message = "Full name must not exceed 200 characters")
        String fullName,

        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone
) {}
