package com.libraryms.dto;

import com.libraryms.model.Author;

import java.time.Instant;
import java.time.LocalDate;

public record AuthorResponse(
        Long id,
        String name,
        String bio,
        LocalDate birthDate,
        Instant createdAt,
        Instant updatedAt
) {}
