package com.libraryms.loan.dto;

import com.libraryms.loan.entity.Loan;

import jakarta.validation.constraints.NotNull;

public record CreateLoanRequest(
        @NotNull(message = "Copy ID is required")
        Long copyId,

        Long memberId
) {}
