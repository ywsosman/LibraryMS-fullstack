package com.libraryms.user.dto;

import com.libraryms.user.entity.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 10, max = 128, message = "New password must be at least 10 characters long")
        String newPassword
) {}
