package com.libraryms.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMemberRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 200, message = "Full name must not exceed 200 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address format")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone
) {}
