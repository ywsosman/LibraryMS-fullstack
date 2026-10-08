package com.libraryms.regression;

import com.libraryms.loan.entity.Loan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.libraryms.author.entity.Author;
import com.libraryms.author.repository.AuthorRepository;
import com.libraryms.book.entity.Book;
import com.libraryms.book.repository.BookRepository;
import com.libraryms.common.security.JwtTokenService;
import com.libraryms.copy.entity.BookCopy;
import com.libraryms.copy.repository.BookCopyRepository;
import com.libraryms.copy.entity.CopyStatus;
import com.libraryms.loan.repository.LoanRepository;
import com.libraryms.loan.dto.CreateLoanRequest;
import com.libraryms.loan.dto.LoanResponse;
import com.libraryms.member.entity.Member;
import com.libraryms.member.repository.MemberRepository;
import com.libraryms.support.IntegrationTest;
import com.libraryms.user.entity.Role;
import com.libraryms.user.entity.RoleName;
import com.libraryms.user.repository.RoleRepository;
import com.libraryms.user.entity.User;
import com.libraryms.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression Test 12:
 * Concurrent borrow of the same book copy by two threads behind a CountDownLatch
 * results in exactly one 201 Created and one 409 Conflict.
 */
@IntegrationTest
class Regression12ConcurrentBorrowIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository copyRepository;

    @Autowired
    private MemberRepository memberRepository;

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
    @DisplayName("Concurrent borrow requests for the same copy produce exactly one 201 and one 409")
    void concurrentBorrowResultsInOneSuccessAndOneConflict() throws Exception {
        // 1. Create ADMIN user for authentication
        User admin = new User("admin_reg12", "admin_reg12@example.com", BCRYPT_HASH);
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        admin.getRoles().add(adminRole);
        admin = userRepository.saveAndFlush(admin);
        String token = jwtTokenService.generateAccessToken(admin);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Setup Author, Book, and single AVAILABLE BookCopy
        Author author = authorRepository.saveAndFlush(new Author("H.G. Wells", "Sci-Fi", null));
        Book book = new Book("The Time Machine", "9780451528551", LocalDate.of(1895, 5, 7));
        book.replaceAuthors(Set.of(author));
        book = bookRepository.saveAndFlush(book);

        BookCopy copy = new BookCopy(book, "BC-CONCURRENT-001");
        copy.setStatus(CopyStatus.AVAILABLE);
        copy = copyRepository.saveAndFlush(copy);
        Long copyId = copy.getId();

        // 3. Setup two distinct members
        Member member1 = memberRepository.saveAndFlush(new Member("Borrower One", "b1_reg12@example.com", null));
        Member member2 = memberRepository.saveAndFlush(new Member("Borrower Two", "b2_reg12@example.com", null));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<ResponseEntity<String>> responses = Collections.synchronizedList(new ArrayList<>());

        try {
            Future<?> f1 = executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    CreateLoanRequest req = new CreateLoanRequest(copyId, member1.getId());
                    HttpEntity<CreateLoanRequest> entity = new HttpEntity<>(req, headers);
                    ResponseEntity<String> res = restTemplate.postForEntity("/api/v1/loans", entity, String.class);
                    responses.add(res);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Future<?> f2 = executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    CreateLoanRequest req = new CreateLoanRequest(copyId, member2.getId());
                    HttpEntity<CreateLoanRequest> entity = new HttpEntity<>(req, headers);
                    ResponseEntity<String> res = restTemplate.postForEntity("/api/v1/loans", entity, String.class);
                    responses.add(res);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Wait for both worker threads to be ready at the gate
            boolean ready = readyLatch.await(5, TimeUnit.SECONDS);
            assertThat(ready).isTrue();

            // Release both threads simultaneously
            startLatch.countDown();

            f1.get(10, TimeUnit.SECONDS);
            f2.get(10, TimeUnit.SECONDS);

            // 4. Verify outcomes: exactly one 201 Created and one 409 Conflict
            assertThat(responses).hasSize(2);
            long count201 = responses.stream().filter(r -> r.getStatusCode() == HttpStatus.CREATED).count();
            long count409 = responses.stream().filter(r -> r.getStatusCode() == HttpStatus.CONFLICT).count();

            assertThat(count201).isEqualTo(1L);
            assertThat(count409).isEqualTo(1L);

            // 5. Verify database invariants
            BookCopy updatedCopy = copyRepository.findById(copyId).orElseThrow();
            assertThat(updatedCopy.getStatus()).isEqualTo(CopyStatus.ON_LOAN);

            boolean hasOpenLoan = loanRepository.existsByCopyIdAndReturnedAtIsNull(copyId);
            assertThat(hasOpenLoan).isTrue();

        } finally {
            executor.shutdownNow();
            // Cleanup test data to prevent cross-test interference
            loanRepository.findAll().stream()
                    .filter(l -> l.getCopy().getId().equals(copyId))
                    .forEach(l -> loanRepository.deleteById(l.getId()));
            copyRepository.deleteById(copyId);
            bookRepository.deleteById(book.getId());
            authorRepository.deleteById(author.getId());
            memberRepository.deleteById(member1.getId());
            memberRepository.deleteById(member2.getId());
            userRepository.deleteById(admin.getId());
        }
    }
}
