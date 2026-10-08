package com.libraryms.regression;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.libraryms.author.entity.Author;
import com.libraryms.author.repository.AuthorRepository;
import com.libraryms.book.entity.Book;
import com.libraryms.book.repository.BookRepository;
import com.libraryms.common.error.ApiErrorResponse;
import com.libraryms.common.security.JwtTokenService;
import com.libraryms.copy.entity.BookCopy;
import com.libraryms.copy.repository.BookCopyRepository;
import com.libraryms.copy.entity.CopyStatus;
import com.libraryms.loan.entity.Loan;
import com.libraryms.loan.repository.LoanRepository;
import com.libraryms.member.entity.Member;
import com.libraryms.member.repository.MemberRepository;
import com.libraryms.member.dto.MemberResponse;
import com.libraryms.support.IntegrationTest;
import com.libraryms.user.entity.Role;
import com.libraryms.user.entity.RoleName;
import com.libraryms.user.repository.RoleRepository;
import com.libraryms.user.entity.User;
import com.libraryms.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression Test 2:
 * Deleting a member does not delete any book or copy;
 * deleting a member who has an open loan returns 409 Conflict.
 */
@IntegrationTest
class Regression02MemberDeleteIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private static final String BCRYPT_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoO.fSjN6f6F6qX.0dZqIuA9cQ8zZ8.Hwe";

    @Test
    @DisplayName("Deleting member with open loan returns 409; soft-deleting member preserves books, copies, and loan history")
    void memberDeletionRulesVerified() {
        // 1. Create ADMIN user and generate JWT
        User admin = new User("admin_reg02", "admin_reg02@example.com", BCRYPT_HASH);
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        admin.getRoles().add(adminRole);
        admin = userRepository.saveAndFlush(admin);
        String adminToken = jwtTokenService.generateAccessToken(admin);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 2. Set up catalog: Author, Book, BookCopy
        Author author = authorRepository.saveAndFlush(new Author("Arthur Conan Doyle", "Bio", null));
        Book book = new Book("A Study in Scarlet", "9780140439083", java.time.LocalDate.of(1887, 11, 1));
        book.replaceAuthors(Set.of(author));
        book = bookRepository.saveAndFlush(book);
        Long bookId = book.getId();

        BookCopy copy1 = new BookCopy(book, "BC-REG02-001");
        copy1.setStatus(CopyStatus.ON_LOAN);
        copy1 = copyRepository.saveAndFlush(copy1);
        Long copy1Id = copy1.getId();

        BookCopy copy2 = new BookCopy(book, "BC-REG02-002");
        copy2.setStatus(CopyStatus.AVAILABLE);
        copy2 = copyRepository.saveAndFlush(copy2);
        Long copy2Id = copy2.getId();

        // 3. Create Member A and an open loan for copy1
        Member memberA = memberRepository.saveAndFlush(new Member("Sherlock Holmes", "sherlock@221b.uk", "555-1234"));
        Long memberAId = memberA.getId();

        Instant now = Instant.now();
        Loan openLoan = new Loan(copy1, memberA, now, now.plus(14, ChronoUnit.DAYS));
        openLoan = loanRepository.saveAndFlush(openLoan);
        Long openLoanId = openLoan.getId();

        Member memberB = null;
        Long memberBId = null;
        Loan closedLoan = null;
        Long closedLoanId = null;

        try {
            // 4. Attempt to delete Member A who has an active open loan -> MUST return 409 CONFLICT
            ResponseEntity<ApiErrorResponse> conflictRes = restTemplate.exchange(
                    "/api/v1/members/" + memberAId,
                    HttpMethod.DELETE,
                    entity,
                    ApiErrorResponse.class
            );

            assertThat(conflictRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(conflictRes.getBody()).isNotNull();
            assertThat(conflictRes.getBody().message()).contains("active open loans");

            // Verify Member A is still active and not deleted
            Member memberAAfter = memberRepository.findById(memberAId).orElseThrow();
            assertThat(memberAAfter.getDeletedAt()).isNull();

            // 5. Create Member B with past/no open loan
            memberB = memberRepository.saveAndFlush(new Member("John Watson", "watson@221b.uk", "555-5678"));
            memberBId = memberB.getId();

            // Watson had a past loan that is already returned
            closedLoan = new Loan(copy2, memberB, now.minus(20, ChronoUnit.DAYS), now.minus(6, ChronoUnit.DAYS));
            closedLoan.setReturnedAt(now.minus(7, ChronoUnit.DAYS));
            closedLoan = loanRepository.saveAndFlush(closedLoan);
            closedLoanId = closedLoan.getId();

            // 6. Delete Member B -> MUST succeed with 204 NO_CONTENT (soft delete)
            ResponseEntity<Void> deleteRes = restTemplate.exchange(
                    "/api/v1/members/" + memberBId,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            assertThat(deleteRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // 7. Verify soft deletion: deleted_at is set, active queries no longer return Member B
            Member memberBAfter = memberRepository.findById(memberBId).orElseThrow();
            assertThat(memberBAfter.getDeletedAt()).isNotNull();

            ResponseEntity<ApiErrorResponse> getDeletedRes = restTemplate.exchange(
                    "/api/v1/members/" + memberBId,
                    HttpMethod.GET,
                    entity,
                    ApiErrorResponse.class
            );
            assertThat(getDeletedRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

            // 8. CRITICAL: Verify that deleting member did NOT delete any book or book copy!
            assertThat(bookRepository.findById(bookId)).isPresent();
            assertThat(copyRepository.findById(copy1Id)).isPresent();
            assertThat(copyRepository.findById(copy2Id)).isPresent();

            // 9. CRITICAL: Verify that closed loan history for the deleted member is preserved in DB!
            assertThat(loanRepository.findById(closedLoanId)).isPresent();
            assertThat(loanRepository.findById(openLoanId)).isPresent();
        } finally {
            // Clean up admin user and test data so Regression13StartupFailFastIT verifies zero bootstrap admins
            if (admin != null) userRepository.deleteById(admin.getId());
            if (openLoanId != null) loanRepository.deleteById(openLoanId);
            if (closedLoanId != null) loanRepository.deleteById(closedLoanId);
            if (copy1Id != null) copyRepository.deleteById(copy1Id);
            if (copy2Id != null) copyRepository.deleteById(copy2Id);
            if (bookId != null) bookRepository.deleteById(bookId);
            if (author != null) authorRepository.deleteById(author.getId());
            if (memberAId != null) memberRepository.deleteById(memberAId);
            if (memberBId != null) memberRepository.deleteById(memberBId);
        }
    }
}
