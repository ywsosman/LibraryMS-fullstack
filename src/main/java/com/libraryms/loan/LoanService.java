package com.libraryms.loan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.common.error.BadRequestException;
import com.libraryms.common.error.ConflictException;
import com.libraryms.common.error.ResourceNotFoundException;
import com.libraryms.common.security.CurrentUserPrincipal;
import com.libraryms.copy.BookCopy;
import com.libraryms.copy.BookCopyRepository;
import com.libraryms.copy.CopyStatus;
import com.libraryms.loan.dto.CreateLoanRequest;
import com.libraryms.loan.dto.LoanResponse;
import com.libraryms.loan.dto.LoanStatusFilter;
import com.libraryms.member.Member;
import com.libraryms.member.MemberRepository;
import com.libraryms.user.RoleName;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookCopyRepository copyRepository;
    private final MemberRepository memberRepository;
    private final LoanMapper loanMapper;
    private final LoanProperties loanProperties;
    private final Clock clock;

    public LoanService(LoanRepository loanRepository,
                       BookCopyRepository copyRepository,
                       MemberRepository memberRepository,
                       LoanMapper loanMapper,
                       LoanProperties loanProperties,
                       Clock clock) {
        this.loanRepository = loanRepository;
        this.copyRepository = copyRepository;
        this.memberRepository = memberRepository;
        this.loanMapper = loanMapper;
        this.loanProperties = loanProperties;
        this.clock = clock;
    }

    @Transactional
    public LoanResponse createLoan(CreateLoanRequest request, CurrentUserPrincipal principal) {
        final Long effectiveMemberId;
        if (request.memberId() == null) {
            if (principal == null || principal.memberId() == null) {
                throw new BadRequestException("Current user is not linked to any member profile. Specify memberId.");
            }
            effectiveMemberId = principal.memberId();
        } else {
            if (principal != null && !principal.hasRole(RoleName.ROLE_ADMIN) && !request.memberId().equals(principal.memberId())) {
                throw new BadRequestException("You may only borrow for your own linked member profile");
            }
            effectiveMemberId = request.memberId();
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(effectiveMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", effectiveMemberId));

        long openLoans = loanRepository.countByMemberIdAndReturnedAtIsNull(effectiveMemberId);
        if (openLoans >= loanProperties.getMaxOpenLoans()) {
            throw new ConflictException("Member has reached maximum open loans limit (" + loanProperties.getMaxOpenLoans() + ")");
        }

        BookCopy copy = copyRepository.findWithBookById(request.copyId())
                .orElseThrow(() -> new ResourceNotFoundException("BookCopy", request.copyId()));

        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new ConflictException("Book copy with barcode " + copy.getBarcode() + " is not available for borrowing (status: " + copy.getStatus() + ")");
        }

        copy.setStatus(CopyStatus.ON_LOAN);

        Instant now = clock.instant();
        Instant dueDate = now.plus(loanProperties.getDurationDays(), ChronoUnit.DAYS);

        Loan loan = new Loan(copy, member, now, dueDate);
        Loan saved = loanRepository.save(loan);
        return loanMapper.toResponse(saved);
    }

    @Transactional
    public LoanResponse returnLoan(Long id) {
        Loan loan = loanRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));

        if (!loan.isOpen()) {
            throw new ConflictException("Loan has already been returned");
        }

        Instant now = clock.instant();
        loan.setReturnedAt(now);

        if (now.isAfter(loan.getDueDate())) {
            long overdueDays = Duration.between(loan.getDueDate(), now).toDays();
            if (overdueDays == 0) {
                overdueDays = 1;
            }
            BigDecimal fine = loanProperties.getFinePerDay()
                    .multiply(BigDecimal.valueOf(overdueDays))
                    .setScale(2, RoundingMode.HALF_UP);
            loan.setFineAmount(fine);
        } else {
            loan.setFineAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        BookCopy copy = loan.getCopy();
        copy.setStatus(CopyStatus.AVAILABLE);

        return loanMapper.toResponse(loan);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanById(Long id) {
        Loan loan = loanRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
        return loanMapper.toResponse(loan);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> listLoans(Long memberId, Long copyId, LoanStatusFilter status, Pageable pageable) {
        Specification<Loan> spec = (root, query, cb) -> cb.conjunction();

        if (memberId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("member").get("id"), memberId));
        }
        if (copyId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("copy").get("id"), copyId));
        }
        if (status != null && status != LoanStatusFilter.ALL) {
            Instant now = clock.instant();
            switch (status) {
                case OPEN -> spec = spec.and((root, query, cb) -> cb.isNull(root.get("returnedAt")));
                case RETURNED -> spec = spec.and((root, query, cb) -> cb.isNotNull(root.get("returnedAt")));
                case OVERDUE -> spec = spec.and((root, query, cb) ->
                        cb.and(cb.isNull(root.get("returnedAt")), cb.lessThan(root.get("dueDate"), now)));
            }
        }

        return loanRepository.findAll(spec, pageable).map(loanMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> listMyLoans(Long memberId, Pageable pageable) {
        if (memberId == null) {
            throw new ResourceNotFoundException("Member profile for current user not found");
        }
        return listLoans(memberId, null, LoanStatusFilter.ALL, pageable);
    }
}
