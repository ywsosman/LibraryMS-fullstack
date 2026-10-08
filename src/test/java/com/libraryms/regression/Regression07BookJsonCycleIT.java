package com.libraryms.regression;

import java.time.Instant;
import java.time.LocalDate;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryms.author.entity.Author;
import com.libraryms.author.repository.AuthorRepository;
import com.libraryms.book.entity.Book;
import com.libraryms.book.repository.BookRepository;
import com.libraryms.book.dto.BookResponse;
import com.libraryms.common.security.JwtTokenService;
import com.libraryms.copy.entity.BookCopy;
import com.libraryms.copy.repository.BookCopyRepository;
import com.libraryms.copy.entity.CopyStatus;
import com.libraryms.copy.dto.CopyResponse;
import com.libraryms.loan.entity.Loan;
import com.libraryms.loan.repository.LoanRepository;
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
 * Regression Test 7:
 * Book JSON serialization with borrowed copies has no circular reference cycle
 * and does not leak raw JPA entities or Hibernate proxies.
 */
@IntegrationTest
class Regression07BookJsonCycleIT {

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

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BCRYPT_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoO.fSjN6f6F6qX.0dZqIuA9cQ8zZ8.Hwe";

    @Test
    @DisplayName("Serializing Book and Copies with active loans produces valid acyclic JSON with no JPA leakage")
    void bookJsonSerializationHasNoCyclesOrEntityLeakage() throws Exception {
        // 1. Authenticate user
        User user = new User("user_reg07", "user_reg07@example.com", BCRYPT_HASH);
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();
        user.getRoles().add(userRole);
        user = userRepository.saveAndFlush(user);
        String token = jwtTokenService.generateAccessToken(user);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 2. Persist full aggregate graph: Author -> Book -> BookCopy -> Loan -> Member
        Author author = authorRepository.saveAndFlush(new Author("Frank Herbert", "Sci-Fi author", LocalDate.of(1920, 10, 8)));
        Book book = new Book("Dune", "9780441172719", LocalDate.of(1965, 8, 1));
        book.replaceAuthors(Set.of(author));
        book.replaceGenres(Set.of("Science Fiction", "Adventure"));
        book = bookRepository.saveAndFlush(book);
        Long bookId = book.getId();

        BookCopy copy1 = new BookCopy(book, "BC-DUNE-001");
        copy1.setStatus(CopyStatus.ON_LOAN);
        copy1 = copyRepository.saveAndFlush(copy1);

        BookCopy copy2 = new BookCopy(book, "BC-DUNE-002");
        copy2.setStatus(CopyStatus.AVAILABLE);
        copy2 = copyRepository.saveAndFlush(copy2);

        Member member = memberRepository.saveAndFlush(new Member("Paul Atreides", "paul@arrakis.space", "555-0199"));

        Instant now = Instant.now();
        Loan loan = new Loan(copy1, member, now, now.plus(14, ChronoUnit.DAYS));
        loanRepository.saveAndFlush(loan);

        try {
            // 3. GET /api/v1/books/{id}
            ResponseEntity<String> bookRawResponse = restTemplate.exchange(
                    "/api/v1/books/" + bookId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertThat(bookRawResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            String bookJson = bookRawResponse.getBody();
            assertThat(bookJson).isNotNull();

            // Must deserialize cleanly without infinite cycle or Jackson mapping exceptions
            JsonNode bookTree = objectMapper.readTree(bookJson);
            assertThat(bookTree.has("id")).isTrue();
            assertThat(bookTree.get("id").asLong()).isEqualTo(bookId);
            assertThat(bookTree.get("title").asText()).isEqualTo("Dune");
            assertThat(bookTree.get("isbn").asText()).isEqualTo("9780441172719");
            assertThat(bookTree.get("totalCopies").asLong()).isEqualTo(2);
            assertThat(bookTree.get("availableCopies").asLong()).isEqualTo(1);

            // Verify NO entity leakages or proxy internals
            assertThat(bookJson).doesNotContain("hibernateLazyInitializer");
            assertThat(bookJson).doesNotContain("handler");
            assertThat(bookJson).doesNotContain("passwordHash");
            assertThat(bookJson).doesNotContain("borrowedAt");

            // 4. GET /api/v1/books/{id}/copies
            ResponseEntity<String> copiesRawResponse = restTemplate.exchange(
                    "/api/v1/books/" + bookId + "/copies",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertThat(copiesRawResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            String copiesJson = copiesRawResponse.getBody();
            assertThat(copiesJson).isNotNull();

            JsonNode copiesTree = objectMapper.readTree(copiesJson);
            assertThat(copiesTree.has("content")).isTrue();
            JsonNode content = copiesTree.get("content");
            assertThat(content.isArray()).isTrue();
            assertThat(content.size()).isEqualTo(2);

            // Inspect copy items in content array
            for (JsonNode copyNode : content) {
                assertThat(copyNode.has("barcode")).isTrue();
                assertThat(copyNode.has("status")).isTrue();
                assertThat(copyNode.has("book")).isTrue();

                JsonNode bookSummary = copyNode.get("book");
                assertThat(bookSummary.has("id")).isTrue();
                assertThat(bookSummary.has("title")).isTrue();
                assertThat(bookSummary.has("isbn")).isTrue();

                // BookSummary MUST NOT contain recursive copies or loan trees
                assertThat(bookSummary.has("copies")).isFalse();
                assertThat(bookSummary.has("loans")).isFalse();
            }

            // 5. GET /api/v1/copies/{id}
            ResponseEntity<CopyResponse> copyResponse = restTemplate.exchange(
                    "/api/v1/copies/" + copy1.getId(),
                    HttpMethod.GET,
                    entity,
                    CopyResponse.class
            );

            assertThat(copyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            CopyResponse copyBody = copyResponse.getBody();
            assertThat(copyBody).isNotNull();
            assertThat(copyBody.barcode()).isEqualTo("BC-DUNE-001");
            assertThat(copyBody.status()).isEqualTo(CopyStatus.ON_LOAN);
            assertThat(copyBody.book().id()).isEqualTo(bookId);
        } finally {
            loanRepository.delete(loan);
            copyRepository.delete(copy1);
            copyRepository.delete(copy2);
            bookRepository.delete(book);
            authorRepository.delete(author);
            memberRepository.delete(member);
            userRepository.delete(user);
        }
    }
}
