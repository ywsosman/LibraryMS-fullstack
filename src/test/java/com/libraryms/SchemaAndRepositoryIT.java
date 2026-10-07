package com.libraryms;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.author.Author;
import com.libraryms.author.AuthorRepository;
import com.libraryms.book.Book;
import com.libraryms.book.BookRepository;
import com.libraryms.copy.BookCopy;
import com.libraryms.copy.BookCopyRepository;
import com.libraryms.copy.CopyStatus;
import com.libraryms.loan.Loan;
import com.libraryms.loan.LoanRepository;
import com.libraryms.member.Member;
import com.libraryms.member.MemberRepository;
import com.libraryms.support.IntegrationTest;
import com.libraryms.user.Role;
import com.libraryms.user.RoleName;
import com.libraryms.user.RoleRepository;
import com.libraryms.user.User;
import com.libraryms.user.UserRepository;

@IntegrationTest
class SchemaAndRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

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

    // Valid 60-char BCrypt hash conforming to ck_users_password_is_bcrypt regex
    private static final String BCRYPT_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoO.fSjN6f6F6qX.0dZqIuA9cQ8zZ8.Hwe";

    @Test
    @DisplayName("Roles are seeded by Flyway and can be assigned to users")
    @Transactional
    void rolesAreSeededAndAssignedToUser() {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();

        assertThat(userRole.getId()).isNotNull();
        assertThat(adminRole.getId()).isNotNull();

        User user = new User("adminuser_schema_it", "admin_schema_it@example.com", BCRYPT_HASH);
        user.addRole(adminRole);
        user.addRole(userRole);
        user = userRepository.saveAndFlush(user);

        User loaded = userRepository.findWithRolesById(user.getId()).orElseThrow();
        assertThat(loaded.hasRole(RoleName.ROLE_ADMIN)).isTrue();
        assertThat(loaded.hasRole(RoleName.ROLE_USER)).isTrue();
    }

    @Test
    @DisplayName("Books with authors and genres are persisted and retrieved correctly")
    @Transactional
    void bookWithAuthorsAndGenres() {
        Author author = authorRepository.saveAndFlush(new Author("Martin Fowler SchemaIT"));

        Book book = new Book("Refactoring SchemaIT", "9780201485677", LocalDate.of(1999, 7, 8));
        book.replaceAuthors(Set.of(author));
        book.replaceGenres(Set.of("Software Engineering", "Computer Science"));
        book = bookRepository.saveAndFlush(book);

        Book loaded = bookRepository.findDetailedById(book.getId()).orElseThrow();
        assertThat(loaded.getAuthors()).extracting(Author::getName).containsExactly("Martin Fowler SchemaIT");
        assertThat(loaded.getGenres()).containsExactlyInAnyOrder("Software Engineering", "Computer Science");
        assertThat(loaded.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("Copies reference a book and track status")
    @Transactional
    void bookCopyWithStatus() {
        Book book = bookRepository.saveAndFlush(new Book("Clean Code SchemaIT", "9780132350891", LocalDate.of(2008, 8, 1)));

        BookCopy copy = new BookCopy(book, "BC-CLEAN-SCHEMAIT-001");
        copy = copyRepository.saveAndFlush(copy);

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.AVAILABLE);
        assertThat(copyRepository.countByBookId(book.getId())).isEqualTo(1);
        assertThat(copyRepository.countByBookIdAndStatus(book.getId(), CopyStatus.AVAILABLE)).isEqualTo(1);
    }

    @Test
    @DisplayName("Loans link copies to members with optimistic locking version")
    @Transactional
    void loanLifecycle() {
        Book book = bookRepository.saveAndFlush(new Book("Domain-Driven Design SchemaIT", "9780321125217", LocalDate.of(2003, 8, 30)));
        BookCopy copy = copyRepository.saveAndFlush(new BookCopy(book, "BC-DDD-SCHEMAIT-001"));
        Member member = memberRepository.saveAndFlush(new Member("Alice Smith SchemaIT", "alice_schemait@example.com", "555-0100"));

        Instant now = Instant.now();
        Instant due = now.plusSeconds(14 * 86400);
        Loan loan = new Loan(copy, member, now, due);
        loan = loanRepository.saveAndFlush(loan);

        assertThat(loan.getId()).isNotNull();
        assertThat(loan.isOpen()).isTrue();
        assertThat(loanRepository.existsByMemberIdAndReturnedAtIsNull(member.getId())).isTrue();
        assertThat(loanRepository.existsByCopyIdAndReturnedAtIsNull(copy.getId())).isTrue();
    }
}
