package com.libraryms.copy.dto;

import com.libraryms.book.entity.Book;

import java.time.Instant;

import com.libraryms.common.dto.summary.BookSummary;
import com.libraryms.copy.entity.CopyStatus;

public record CopyResponse(
        Long id,
        BookSummary book,
        String barcode,
        CopyStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
