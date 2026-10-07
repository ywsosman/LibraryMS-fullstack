package com.libraryms.author.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record CreateAuthorRequest(
        @NotBlank(message = "Author name is required")
        @Size(max = 200, message = "Author name cannot exceed 200 characters")
        String name,

        String bio,

        @Past(message = "Birth date must be in the past")
        LocalDate birthDate
) {}
