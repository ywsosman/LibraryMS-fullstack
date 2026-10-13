package com.libraryms.dto;

import com.libraryms.model.Book;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;


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
