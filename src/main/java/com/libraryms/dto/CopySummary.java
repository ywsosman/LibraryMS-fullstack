package com.libraryms.dto;

import com.libraryms.enums.CopyStatus;

public record CopySummary(
        Long id,
        String barcode,
        CopyStatus status
) {}
