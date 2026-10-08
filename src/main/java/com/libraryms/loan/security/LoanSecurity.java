package com.libraryms.loan.security;

import com.libraryms.loan.entity.Loan;
import com.libraryms.loan.repository.LoanRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.libraryms.common.security.CurrentUserPrincipal;

@Component("loanSecurity")
public class LoanSecurity {

    private final LoanRepository loanRepository;

    public LoanSecurity(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public boolean isLoanMember(Authentication authentication, Long loanId) {
        if (authentication == null || loanId == null) {
            return false;
        }
        if (authentication.getPrincipal() instanceof CurrentUserPrincipal principal) {
            if (principal.memberId() == null) {
                return false;
            }
            return loanRepository.findById(loanId)
                    .map(loan -> principal.memberId().equals(loan.getMember().getId()))
                    .orElse(false);
        }
        return false;
    }
}
