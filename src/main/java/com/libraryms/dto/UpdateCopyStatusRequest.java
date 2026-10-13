package com.libraryms.dto;

import com.libraryms.enums.CopyStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateCopyStatusRequest(
        @NotNull(message = "Status is required")
        CopyStatus status
) {}
