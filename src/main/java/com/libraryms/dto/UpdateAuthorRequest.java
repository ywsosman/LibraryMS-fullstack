package com.libraryms.dto;

import com.libraryms.model.Author;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record UpdateAuthorRequest(
        @NotBlank(message = "Author name is required")
        @Size(max = 200, message = "Author name cannot exceed 200 characters")
        String name,

        @Size(max = 10000, message = "Bio cannot exceed 10,000 characters")
        String bio,

        @Past(message = "Birth date must be in the past")
        LocalDate birthDate
) {}
