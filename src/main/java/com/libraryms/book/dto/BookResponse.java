package com.libraryms.book.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import com.libraryms.common.dto.summary.AuthorSummary;

public record BookResponse(
        Long id,
        String title,
        String isbn,
        LocalDate publishedDate,
        String description,
        Set<AuthorSummary> authors,
        Set<String> genres,
        long totalCopies,
        long availableCopies,
        Instant createdAt,
        Instant updatedAt
) {}
