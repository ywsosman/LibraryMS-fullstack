package com.libraryms.book.dto;

import java.time.LocalDate;
import java.util.Set;

import com.libraryms.common.validation.ValidIsbn;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBookRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title cannot exceed 500 characters")
        String title,

        @NotBlank(message = "ISBN is required")
        @ValidIsbn
        String isbn,

        @NotNull(message = "Published date is required")
        LocalDate publishedDate,

        String description,

        @NotEmpty(message = "At least one author ID is required")
        Set<Long> authorIds,

        Set<String> genres
) {}
