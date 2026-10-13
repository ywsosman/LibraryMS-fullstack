package com.libraryms.dto;

import com.libraryms.model.Author;
import com.libraryms.model.Book;

import java.time.LocalDate;
import java.util.Set;

import com.libraryms.config.ValidIsbn;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title cannot exceed 500 characters")
        String title,

        @NotBlank(message = "ISBN is required")
        @ValidIsbn
        String isbn,

        @NotNull(message = "Published date is required")
        LocalDate publishedDate,

        @Size(max = 10000, message = "Description cannot exceed 10,000 characters")
        String description,

        @NotEmpty(message = "At least one author ID is required")
        Set<Long> authorIds,

        Set<@NotBlank(message = "Genre cannot be blank") @Size(max = 50, message = "Genre cannot exceed 50 characters") String> genres
) {}
