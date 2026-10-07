package com.libraryms.loan.dto;

import jakarta.validation.constraints.NotNull;

public record CreateLoanRequest(
        @NotNull(message = "Copy ID is required")
        Long copyId,

        Long memberId
) {}
