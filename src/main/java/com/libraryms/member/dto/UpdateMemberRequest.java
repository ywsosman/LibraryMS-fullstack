package com.libraryms.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateMemberRequest(
        @Size(min = 1, max = 200, message = "Full name must be between 1 and 200 characters")
        String fullName,

        @Email(message = "Invalid email address format")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone
) {}
