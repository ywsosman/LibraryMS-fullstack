package com.libraryms.common.dto.summary;

import com.libraryms.copy.entity.CopyStatus;

public record CopySummary(
        Long id,
        String barcode,
        CopyStatus status
) {}
