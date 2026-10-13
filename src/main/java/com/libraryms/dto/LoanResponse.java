package com.libraryms.dto;

import com.libraryms.model.Loan;
import com.libraryms.model.Book;
import com.libraryms.model.Member;

import java.math.BigDecimal;
import java.time.Instant;


public record LoanResponse(
        Long id,
        CopySummary copy,
        BookSummary book,
        MemberSummary member,
        Instant borrowedAt,
        Instant dueDate,
        Instant returnedAt,
        BigDecimal fineAmount,
        Instant fineSettledAt,
        boolean open,
        boolean overdue
) {}
