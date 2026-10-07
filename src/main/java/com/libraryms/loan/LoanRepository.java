package com.libraryms.loan;

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
}
