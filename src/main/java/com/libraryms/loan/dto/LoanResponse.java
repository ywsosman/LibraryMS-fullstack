package com.libraryms.loan.dto;

import com.libraryms.loan.entity.Loan;
import com.libraryms.book.entity.Book;
import com.libraryms.member.entity.Member;

import java.math.BigDecimal;
import java.time.Instant;

import com.libraryms.common.dto.summary.BookSummary;
import com.libraryms.common.dto.summary.CopySummary;
import com.libraryms.common.dto.summary.MemberSummary;

public record LoanResponse(
        Long id,
        CopySummary copy,
        BookSummary book,
        MemberSummary member,
        Instant borrowedAt,
        Instant dueDate,
        Instant returnedAt,
        BigDecimal fineAmount,
        boolean open,
        boolean overdue
) {}
