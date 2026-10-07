package com.libraryms.loan;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.libraryms.book.Book;
import com.libraryms.common.dto.summary.BookSummary;
import com.libraryms.common.dto.summary.CopySummary;
import com.libraryms.common.dto.summary.MemberSummary;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookCopyRepository copyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private LoanMapper loanMapper;

    private LoanProperties loanProperties;

    private Clock clock;

    private LoanService loanService;

    private final Instant fixedNow = Instant.parse("2026-10-07T12:00:00Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        loanProperties = new LoanProperties();
        loanProperties.setDurationDays(14);
        loanProperties.setFinePerDay(new BigDecimal("0.50"));
        loanProperties.setMaxOpenLoans(5);

        loanService = new LoanService(
                loanRepository,
                copyRepository,
                memberRepository,
                loanMapper,
                loanProperties,
                clock
        );
    }

    @Test
    @DisplayName("createLoan should succeed when copy is available and member is eligible")
    void createLoan_success() {
        CreateLoanRequest request = new CreateLoanRequest(10L, 5L);
        CurrentUserPrincipal adminPrincipal = new CurrentUserPrincipal(1L, "admin", "admin@example.com", null, Set.of(RoleName.ROLE_ADMIN), List.of());
        Member member = new Member("John Doe", "john@example.com", null);
        BookCopy copy = new BookCopy(new Book("Title", "9780140439083", null), "BC-001");
        copy.setStatus(CopyStatus.AVAILABLE);

        Loan loan = new Loan(copy, member, fixedNow, fixedNow.plus(14, ChronoUnit.DAYS));
        LoanResponse response = new LoanResponse(1L, new CopySummary(10L, "BC-001", CopyStatus.ON_LOAN),
                new BookSummary(1L, "Title", "9780140439083"),
                new MemberSummary(5L, "John Doe"), fixedNow, fixedNow.plus(14, ChronoUnit.DAYS),
                null, BigDecimal.ZERO, true, false);

        when(memberRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(member));
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(5L)).thenReturn(0L);
        when(copyRepository.findWithBookById(10L)).thenReturn(Optional.of(copy));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanMapper.toResponse(loan)).thenReturn(response);

        LoanResponse result = loanService.createLoan(request, adminPrincipal);

        assertThat(result).isNotNull();
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.ON_LOAN);
    }

    @Test
    @DisplayName("createLoan should use principal's memberId when omitted in request")
    void createLoan_defaultsToPrincipalMemberId() {
        CreateLoanRequest request = new CreateLoanRequest(10L, null);
        CurrentUserPrincipal userPrincipal = new CurrentUserPrincipal(2L, "user", "user@example.com", 7L, Set.of(RoleName.ROLE_USER), List.of());
        Member member = new Member("Jane Patron", "jane@example.com", null);
        BookCopy copy = new BookCopy(new Book("Title", "9780140439083", null), "BC-002");
        copy.setStatus(CopyStatus.AVAILABLE);
        Loan loan = new Loan(copy, member, fixedNow, fixedNow.plus(14, ChronoUnit.DAYS));

        when(memberRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(member));
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(7L)).thenReturn(1L);
        when(copyRepository.findWithBookById(10L)).thenReturn(Optional.of(copy));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanMapper.toResponse(loan)).thenReturn(new LoanResponse(1L, null, null, null, fixedNow, null, null, BigDecimal.ZERO, true, false));

        LoanResponse result = loanService.createLoan(request, userPrincipal);

        assertThat(result).isNotNull();
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.ON_LOAN);
    }

    @Test
    @DisplayName("createLoan rejects non-admin borrowing for another member")
    void createLoan_rejectsNonAdminBorrowingForOthers() {
        CreateLoanRequest request = new CreateLoanRequest(10L, 99L);
        CurrentUserPrincipal userPrincipal = new CurrentUserPrincipal(2L, "user", "user@example.com", 7L, Set.of(RoleName.ROLE_USER), List.of());

        assertThatThrownBy(() -> loanService.createLoan(request, userPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own linked member");
    }

    @Test
    @DisplayName("createLoan throws ConflictException when max open loans reached")
    void createLoan_maxOpenLoansReached() {
        CreateLoanRequest request = new CreateLoanRequest(10L, 5L);
        CurrentUserPrincipal adminPrincipal = new CurrentUserPrincipal(1L, "admin", "admin@example.com", null, Set.of(RoleName.ROLE_ADMIN), List.of());
        Member member = new Member("John Doe", "john@example.com", null);

        when(memberRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(member));
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(5L)).thenReturn(5L);

        assertThatThrownBy(() -> loanService.createLoan(request, adminPrincipal))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("maximum open loans");
    }

    @Test
    @DisplayName("createLoan throws ConflictException when copy is already ON_LOAN")
    void createLoan_copyNotAvailable() {
        CreateLoanRequest request = new CreateLoanRequest(10L, 5L);
        CurrentUserPrincipal adminPrincipal = new CurrentUserPrincipal(1L, "admin", "admin@example.com", null, Set.of(RoleName.ROLE_ADMIN), List.of());
        Member member = new Member("John Doe", "john@example.com", null);
        BookCopy copy = new BookCopy(new Book("Title", "9780140439083", null), "BC-001");
        copy.setStatus(CopyStatus.ON_LOAN);

        when(memberRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(member));
        when(loanRepository.countByMemberIdAndReturnedAtIsNull(5L)).thenReturn(0L);
        when(copyRepository.findWithBookById(10L)).thenReturn(Optional.of(copy));

        assertThatThrownBy(() -> loanService.createLoan(request, adminPrincipal))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not available for borrowing");
    }

    @Test
    @DisplayName("returnLoan marks copy AVAILABLE and sets zero fine if on time")
    void returnLoan_onTime() {
        BookCopy copy = new BookCopy(new Book("Title", "9780140439083", null), "BC-001");
        copy.setStatus(CopyStatus.ON_LOAN);
        Member member = new Member("John Doe", "john@example.com", null);
        Loan loan = new Loan(copy, member, fixedNow.minus(7, ChronoUnit.DAYS), fixedNow.plus(7, ChronoUnit.DAYS));

        when(loanRepository.findDetailedById(1L)).thenReturn(Optional.of(loan));
        when(loanMapper.toResponse(loan)).thenReturn(new LoanResponse(1L, null, null, null, null, null, fixedNow, BigDecimal.ZERO, false, false));

        LoanResponse response = loanService.returnLoan(1L);

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        assertThat(loan.getReturnedAt()).isEqualTo(fixedNow);
        assertThat(loan.getFineAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("returnLoan calculates fine correctly if overdue")
    void returnLoan_overdueCalculatesFine() {
        BookCopy copy = new BookCopy(new Book("Title", "9780140439083", null), "BC-001");
        copy.setStatus(CopyStatus.ON_LOAN);
        Member member = new Member("John Doe", "john@example.com", null);
        // Due 4 days ago
        Loan loan = new Loan(copy, member, fixedNow.minus(18, ChronoUnit.DAYS), fixedNow.minus(4, ChronoUnit.DAYS));

        when(loanRepository.findDetailedById(1L)).thenReturn(Optional.of(loan));
        when(loanMapper.toResponse(loan)).thenReturn(new LoanResponse(1L, null, null, null, null, null, fixedNow, new BigDecimal("2.00"), false, false));

        LoanResponse response = loanService.returnLoan(1L);

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        // 4 days overdue * 0.50 = 2.00
        assertThat(loan.getFineAmount()).isEqualByComparingTo(new BigDecimal("2.00"));
    }

    @Test
    @DisplayName("returnLoan throws ConflictException if loan already returned")
    void returnLoan_alreadyReturnedThrowsConflict() {
        BookCopy copy = new BookCopy(new Book("Title", "9780140439083", null), "BC-001");
        Member member = new Member("John Doe", "john@example.com", null);
        Loan loan = new Loan(copy, member, fixedNow.minus(10, ChronoUnit.DAYS), fixedNow.plus(4, ChronoUnit.DAYS));
        loan.setReturnedAt(fixedNow.minus(1, ChronoUnit.DAYS));

        when(loanRepository.findDetailedById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.returnLoan(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been returned");
    }

    @Test
    @DisplayName("getLoanById returns response when loan found")
    void getLoanById_found() {
        Loan loan = new Loan(null, null, fixedNow, fixedNow.plus(14, ChronoUnit.DAYS));
        LoanResponse response = new LoanResponse(1L, null, null, null, fixedNow, null, null, BigDecimal.ZERO, true, false);

        when(loanRepository.findDetailedById(1L)).thenReturn(Optional.of(loan));
        when(loanMapper.toResponse(loan)).thenReturn(response);

        LoanResponse result = loanService.getLoanById(1L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getLoanById throws ResourceNotFoundException when absent")
    void getLoanById_notFound() {
        when(loanRepository.findDetailedById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.getLoanById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listLoans returns paged loans matching filters")
    void listLoans_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Loan loan = new Loan(null, null, fixedNow, fixedNow.plus(14, ChronoUnit.DAYS));
        Page<Loan> page = new PageImpl<>(List.of(loan), pageable, 1);
        LoanResponse response = new LoanResponse(1L, null, null, null, fixedNow, null, null, BigDecimal.ZERO, true, false);

        when(loanRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(loanMapper.toResponse(loan)).thenReturn(response);

        Page<LoanResponse> result = loanService.listLoans(1L, 2L, LoanStatusFilter.OPEN, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("listMyLoans returns paged loans for current member")
    void listMyLoans_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Loan> page = new PageImpl<>(List.of(), pageable, 0);

        when(loanRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<LoanResponse> result = loanService.listMyLoans(5L, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("listMyLoans throws ResourceNotFoundException if memberId is null")
    void listMyLoans_nullMemberIdThrows() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> loanService.listMyLoans(null, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member profile for current user not found");
    }
}
