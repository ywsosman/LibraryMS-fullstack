package com.libraryms.loan.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.libraryms.common.persistence.BaseEntity;
import com.libraryms.copy.entity.BookCopy;
import com.libraryms.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A member borrowing a copy. Loans are never deleted (history is kept forever). */
@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
public class Loan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copy_id", nullable = false, updatable = false)
    private BookCopy copy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "borrowed_at", nullable = false, updatable = false)
    private Instant borrowedAt;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "fine_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    public Loan(BookCopy copy, Member member, Instant borrowedAt, Instant dueDate) {
        this.copy = copy;
        this.member = member;
        this.borrowedAt = borrowedAt;
        this.dueDate = dueDate;
    }

    public boolean isOpen() {
        return returnedAt == null;
    }

    @Override
    public String toString() {
        return "Loan{id=" + getId() + ", borrowedAt=" + borrowedAt + ", returnedAt=" + returnedAt + "}";
    }
}
