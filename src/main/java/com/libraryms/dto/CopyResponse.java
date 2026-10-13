package com.libraryms.dto;

import com.libraryms.model.Book;

import java.time.Instant;

import com.libraryms.enums.CopyStatus;

public record CopyResponse(
        Long id,
        BookSummary book,
        String barcode,
        CopyStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
