package com.libraryms.copy.dto;

import com.libraryms.book.entity.Book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCopyRequest(
        @NotNull(message = "Book ID is required")
        Long bookId,

        @NotBlank(message = "Barcode is required")
        @Size(max = 64, message = "Barcode cannot exceed 64 characters")
        String barcode
) {}
