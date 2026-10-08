package com.libraryms.author.dto;

import com.libraryms.author.entity.Author;

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
