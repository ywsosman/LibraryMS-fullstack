package com.libraryms.regression;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import com.libraryms.auth.dto.AuthResponse;
import com.libraryms.auth.dto.LoginRequest;
import com.libraryms.auth.dto.RegisterRequest;
import com.libraryms.support.IntegrationTest;

/**
 * Regression Test 3:
 * Self-delete returns 204, user can no longer log in, previous token is immediately invalid,
 * and associated audit rows survive with user_id = NULL.
 */
@IntegrationTest
class Regression03SelfDeleteIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Self delete returns 204, invalidates future logins and preserves audit rows")
    void selfDeleteReturns204AndInvalidatesLogin() {
        String username = "selfdeluser";
        String email = "selfdel@example.com";
        String password = "SecurePassword123";

        // 1. Register user
        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        ResponseEntity<AuthResponse> registerRes = restTemplate.postForEntity(
                "/api/v1/auth/register", registerRequest, AuthResponse.class
        );
        assertThat(registerRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = registerRes.getBody().accessToken();

        // 2. Fetch user id from database
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?", Long.class, username
        );

        // 3. Create an audit row referencing this user
        Long auditLogId = jdbcTemplate.queryForObject(
                "INSERT INTO audit_logs (user_id, username, entity_type, entity_id, operation) VALUES (?, ?, 'MEMBER', 1, 'UPDATE') RETURNING id",
                Long.class,
                userId, username
        );

        // 4. Call DELETE /api/v1/users/me
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Void> deleteRes = restTemplate.exchange(
                "/api/v1/users/me",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
        );
        assertThat(deleteRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 5. User is deleted from database
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE id = ?", Integer.class, userId
        );
        assertThat(count).isZero();

        // 6. User can no longer log in
        ResponseEntity<Map> loginRes = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(username, password),
                Map.class
        );
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 7. Old active token immediately rejected by fresh user lookup converter
        ResponseEntity<Map> meRes = restTemplate.exchange(
                "/api/v1/users/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        assertThat(meRes.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 8. Audit row survives with user_id set to NULL and snapshot username intact
        Map<String, Object> auditRow = jdbcTemplate.queryForMap(
                "SELECT user_id, username FROM audit_logs WHERE id = ?", auditLogId
        );
        assertThat(auditRow.get("user_id")).isNull();
        assertThat(auditRow.get("username")).isEqualTo(username);
    }
}
