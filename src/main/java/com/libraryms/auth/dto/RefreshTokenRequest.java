package com.libraryms.auth.dto;

import com.libraryms.auth.entity.RefreshToken;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
