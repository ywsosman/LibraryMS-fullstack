package com.libraryms.regression;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.libraryms.support.IntegrationTest;

/**
 * Regression Test 5:
 * Duplicate username/email/ISBN/barcode rejected by the DATABASE (test with raw SQL insert),
 * not just application code. Also verifies CHECK constraints at database level.
 */
@IntegrationTest
class Regression05DatabaseConstraintIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Valid 60-char BCrypt hash conforming to ck_users_password_is_bcrypt regex
    private static final String BCRYPT_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoO.fSjN6f6F6qX.0dZqIuA9cQ8zZ8.Hwe";

    @Test
    @DisplayName("Duplicate username is rejected by database unique index (case-insensitive)")
    void duplicateUsernameRejectedByDatabase() {
        jdbcTemplate.update(
                "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)",
                "johndoe_reg5", "john1_reg5@example.com", BCRYPT_HASH
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)",
                "JohnDoe_Reg5", "john2_reg5@example.com", BCRYPT_HASH
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Duplicate user email is rejected by database unique index (case-insensitive)")
    void duplicateUserEmailRejectedByDatabase() {
        jdbcTemplate.update(
                "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)",
                "uniqueuser1_reg5", "dup_reg5@example.com", BCRYPT_HASH
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)",
                "uniqueuser2_reg5", "DUP_REG5@EXAMPLE.COM", BCRYPT_HASH
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Duplicate member email is rejected by database unique index (case-insensitive)")
    void duplicateMemberEmailRejectedByDatabase() {
        jdbcTemplate.update(
                "INSERT INTO members (full_name, email) VALUES (?, ?)",
                "Member One Reg5", "patron_reg5@example.com"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO members (full_name, email) VALUES (?, ?)",
                "Member Two Reg5", "PATRON_REG5@EXAMPLE.COM"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Duplicate book ISBN is rejected by database unique constraint")
    void duplicateBookIsbnRejectedByDatabase() {
        jdbcTemplate.update(
                "INSERT INTO books (title, isbn, published_date) VALUES (?, ?, '2023-01-01')",
                "Book One Reg5", "9780134685991"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO books (title, isbn, published_date) VALUES (?, ?, '2023-02-01')",
                "Book Two Reg5", "9780134685991"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Duplicate copy barcode is rejected by database unique constraint")
    void duplicateBarcodeRejectedByDatabase() {
        Long bookId = jdbcTemplate.queryForObject(
                "INSERT INTO books (title, isbn, published_date) VALUES ('Barcoded Book Reg5', '9780321356680', '2020-01-01') RETURNING id",
                Long.class
        );

        jdbcTemplate.update(
                "INSERT INTO book_copies (book_id, barcode, status) VALUES (?, ?, 'AVAILABLE')",
                bookId, "BARCODE-REG5-001"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO book_copies (book_id, barcode, status) VALUES (?, ?, 'AVAILABLE')",
                bookId, "BARCODE-REG5-001"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Invalid password format is rejected by CHECK constraint (must be BCrypt)")
    void nonBcryptPasswordRejectedByDatabase() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (username, email, password_hash) VALUES ('badpass_reg5', 'badpass_reg5@example.com', 'plaintextpassword')"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Loan due date earlier than borrow date is rejected by CHECK constraint")
    void loanDueDateBeforeBorrowedAtRejected() {
        Long bookId = jdbcTemplate.queryForObject(
                "INSERT INTO books (title, isbn, published_date) VALUES ('Check Book Reg5', '9780132350881', '2020-01-01') RETURNING id",
                Long.class
        );
        Long copyId = jdbcTemplate.queryForObject(
                "INSERT INTO book_copies (book_id, barcode, status) VALUES (?, 'BARCODE-CHECK-REG5', 'AVAILABLE') RETURNING id",
                Long.class, bookId
        );
        Long memberId = jdbcTemplate.queryForObject(
                "INSERT INTO members (full_name, email) VALUES ('Check Member Reg5', 'checkmember_reg5@example.com') RETURNING id",
                Long.class
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO loans (copy_id, member_id, borrowed_at, due_date) VALUES (?, ?, now(), now() - INTERVAL '1 day')",
                copyId, memberId
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Loan negative fine amount is rejected by CHECK constraint")
    void negativeFineRejected() {
        Long bookId = jdbcTemplate.queryForObject(
                "INSERT INTO books (title, isbn, published_date) VALUES ('Fine Book Reg5', '9780132350882', '2020-01-01') RETURNING id",
                Long.class
        );
        Long copyId = jdbcTemplate.queryForObject(
                "INSERT INTO book_copies (book_id, barcode, status) VALUES (?, 'BARCODE-FINE-REG5', 'AVAILABLE') RETURNING id",
                Long.class, bookId
        );
        Long memberId = jdbcTemplate.queryForObject(
                "INSERT INTO members (full_name, email) VALUES ('Fine Member Reg5', 'finemember_reg5@example.com') RETURNING id",
                Long.class
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO loans (copy_id, member_id, borrowed_at, due_date, fine_amount) VALUES (?, ?, now(), now() + INTERVAL '14 days', -5.00)",
                copyId, memberId
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Partial unique index prevents two simultaneous open loans for the same copy")
    void duplicateOpenLoanOnSameCopyRejected() {
        Long bookId = jdbcTemplate.queryForObject(
                "INSERT INTO books (title, isbn, published_date) VALUES ('Open Loan Book Reg5', '9780132350883', '2020-01-01') RETURNING id",
                Long.class
        );
        Long copyId = jdbcTemplate.queryForObject(
                "INSERT INTO book_copies (book_id, barcode, status) VALUES (?, 'BARCODE-OPEN-REG5', 'AVAILABLE') RETURNING id",
                Long.class, bookId
        );
        Long member1Id = jdbcTemplate.queryForObject(
                "INSERT INTO members (full_name, email) VALUES ('Open Mem 1 Reg5', 'open1_reg5@example.com') RETURNING id",
                Long.class
        );
        Long member2Id = jdbcTemplate.queryForObject(
                "INSERT INTO members (full_name, email) VALUES ('Open Mem 2 Reg5', 'open2_reg5@example.com') RETURNING id",
                Long.class
        );

        // First open loan succeeds
        jdbcTemplate.update(
                "INSERT INTO loans (copy_id, member_id, borrowed_at, due_date) VALUES (?, ?, now(), now() + INTERVAL '7 days')",
                copyId, member1Id
        );

        // Second open loan on same copy must fail at DB level
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO loans (copy_id, member_id, borrowed_at, due_date) VALUES (?, ?, now(), now() + INTERVAL '7 days')",
                copyId, member2Id
        )).isInstanceOf(DataAccessException.class);
    }
}
