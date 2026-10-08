package com.libraryms.regression;

import com.libraryms.book.entity.Book;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.libraryms.audit.entity.AuditLog;
import com.libraryms.audit.repository.AuditLogRepository;
import com.libraryms.support.IntegrationTest;
import com.libraryms.user.entity.User;
import com.libraryms.user.repository.UserRepository;

/**
 * Regression Test 6:
 * Deleting a user who has audit rows succeeds. The audit log record survives,
 * with user_id set to NULL and the username snapshot intact.
 */
@IntegrationTest
class Regression06UserAuditSurvivalIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Valid 60-char BCrypt hash conforming to ck_users_password_is_bcrypt regex
    private static final String BCRYPT_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoO.fSjN6f6F6qX.0dZqIuA9cQ8zZ8.Hwe";

    @Test
    @DisplayName("Deleting a user via JPA succeeds even when audit rows reference that user")
    void deletingUserWithAuditRowsSucceeds() {
        // 1. Create and persist user
        User user = new User("auditeduser_reg6", "audited_reg6@example.com", BCRYPT_HASH);
        user = userRepository.saveAndFlush(user);
        Long userId = user.getId();

        // 2. Create audit log row
        AuditLog auditLog = new AuditLog(
                userId,
                "auditeduser_reg6",
                "BOOK",
                100L,
                "CREATE",
                Instant.now(),
                "Created book 100"
        );
        auditLog = auditLogRepository.saveAndFlush(auditLog);
        Long auditLogId = auditLog.getId();

        // 3. Delete user
        userRepository.deleteById(userId);
        userRepository.flush();

        // 4. Assert user is deleted
        assertThat(userRepository.findById(userId)).isEmpty();

        // 5. Assert audit log row survives with user_id = null and username intact
        AuditLog survivingAudit = auditLogRepository.findById(auditLogId).orElseThrow();
        assertThat(survivingAudit.getUserId()).isNull();
        assertThat(survivingAudit.getUsername()).isEqualTo("auditeduser_reg6");
        assertThat(survivingAudit.getEntityType()).isEqualTo("BOOK");

        // Raw SQL verification as well
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT user_id, username, entity_type FROM audit_logs WHERE id = ?",
                auditLogId
        );
        assertThat(row.get("user_id")).isNull();
        assertThat(row.get("username")).isEqualTo("auditeduser_reg6");
    }
}
