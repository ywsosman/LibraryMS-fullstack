package com.libraryms.dto;

import com.libraryms.model.Loan;

import jakarta.validation.constraints.NotNull;

public record CreateLoanRequest(
        @NotNull(message = "Copy ID is required")
        Long copyId,

        Long memberId
) {}
