package com.libraryms.copy.dto;

import com.libraryms.copy.entity.CopyStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateCopyStatusRequest(
        @NotNull(message = "Status is required")
        CopyStatus status
) {}
