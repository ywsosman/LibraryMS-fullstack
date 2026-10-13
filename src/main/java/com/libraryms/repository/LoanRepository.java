package com.libraryms.repository;

import com.libraryms.model.Loan;
import com.libraryms.model.Book;
import com.libraryms.model.Member;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    Optional<Loan> findDetailedById(Long id);

    boolean existsByMemberIdAndReturnedAtIsNull(Long memberId);

    long countByMemberIdAndReturnedAtIsNull(Long memberId);

    boolean existsByCopyId(Long copyId);

    boolean existsByCopyIdAndReturnedAtIsNull(Long copyId);

    boolean existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(Long memberId, java.time.Instant now);

    @org.springframework.data.jpa.repository.Query("SELECT coalesce(sum(l.fineAmount), 0) FROM Loan l WHERE l.member.id = :memberId AND l.fineSettledAt IS NULL")
    java.math.BigDecimal sumUnsettledFinesByMemberId(@org.springframework.data.repository.query.Param("memberId") Long memberId);
}
