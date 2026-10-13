package com.libraryms.service;

import com.libraryms.mapper.LoanMapper;
import com.libraryms.config.LoanProperties;
import com.libraryms.model.Loan;
import com.libraryms.model.User;
import com.libraryms.model.Book;
import com.libraryms.config.Audited;
import com.libraryms.repository.LoanRepository;

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

import com.libraryms.exception.BadRequestException;
import com.libraryms.exception.ConflictException;
import com.libraryms.exception.ResourceNotFoundException;
import com.libraryms.config.CurrentUserPrincipal;
import com.libraryms.model.BookCopy;
import com.libraryms.repository.BookCopyRepository;
import com.libraryms.enums.CopyStatus;
import com.libraryms.dto.CreateLoanRequest;
import com.libraryms.dto.LoanResponse;
import com.libraryms.enums.LoanStatusFilter;
import com.libraryms.model.Member;
import com.libraryms.repository.MemberRepository;
import com.libraryms.enums.RoleName;

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
    @Audited(entityType = "LOAN", operation = "BORROW")
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

        Instant now = clock.instant();
        if (loanRepository.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(effectiveMemberId, now)) {
            throw new ConflictException("Member has overdue loans that must be returned before borrowing new books");
        }
        BigDecimal unsettledFines = loanRepository.sumUnsettledFinesByMemberId(effectiveMemberId);
        if (unsettledFines != null && unsettledFines.compareTo(new BigDecimal("10.00")) > 0) {
            throw new ConflictException("Member has outstanding unpaid fines exceeding the allowed threshold ($10.00)");
        }

        BookCopy copy = copyRepository.findWithBookById(request.copyId())
                .orElseThrow(() -> new ResourceNotFoundException("BookCopy", request.copyId()));

        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new ConflictException("Book copy with barcode " + copy.getBarcode() + " is not available for borrowing (status: " + copy.getStatus() + ")");
        }

        copy.setStatus(CopyStatus.ON_LOAN);

        Instant dueDate = now.plus(loanProperties.getDurationDays(), ChronoUnit.DAYS);

        Loan loan = new Loan(copy, member, now, dueDate);
        Loan saved = loanRepository.save(loan);
        return loanMapper.toResponse(saved);
    }

    @Transactional
    @Audited(entityType = "LOAN", operation = "RETURN")
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

    @Transactional
    @Audited(entityType = "LOAN", operation = "SETTLE_FINE")
    public LoanResponse settleFine(Long id) {
        Loan loan = loanRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));

        if (loan.getFineAmount() == null || loan.getFineAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Loan has no fine to settle");
        }
        if (loan.getFineSettledAt() != null) {
            throw new ConflictException("Fine for this loan has already been settled");
        }

        loan.setFineSettledAt(clock.instant());
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
