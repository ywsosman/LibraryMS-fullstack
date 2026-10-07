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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.libraryms.auth.dto.AuthResponse;
import com.libraryms.auth.dto.LoginRequest;
import com.libraryms.auth.dto.RegisterRequest;
import com.libraryms.support.IntegrationTest;
import com.libraryms.user.dto.ChangePasswordRequest;

/**
 * Regression Test 1:
 * Self password change stores a BCrypt hash in the database, and the new password logs in.
 * The old password can no longer log in.
 */
@IntegrationTest
class Regression01SelfPasswordChangeIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Changing password stores BCrypt hash and allows login with new password")
    void changePasswordStoresBcryptAndAllowsLogin() {
        String username = "passchangeuser";
        String email = "passchange@example.com";
        String oldPassword = "InitialPassword123";
        String newPassword = "UpdatedSecurePassword456";

        // 1. Register user
        RegisterRequest registerRequest = new RegisterRequest(username, email, oldPassword);
        ResponseEntity<AuthResponse> registerRes = restTemplate.postForEntity(
                "/api/v1/auth/register", registerRequest, AuthResponse.class
        );
        assertThat(registerRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String accessToken = registerRes.getBody().accessToken();

        // 2. Change password via PUT /api/v1/users/me/password
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);

        ChangePasswordRequest changeReq = new ChangePasswordRequest(oldPassword, newPassword);
        ResponseEntity<Void> changeRes = restTemplate.exchange(
                "/api/v1/users/me/password",
                HttpMethod.PUT,
                new HttpEntity<>(changeReq, authHeaders),
                Void.class
        );
        assertThat(changeRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3. Inspect database directly: password hash must be a valid BCrypt hash matching new password
        String dbHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE username = ?",
                String.class,
                username
        );
        assertThat(dbHash).isNotNull();
        assertThat(dbHash).startsWith("$2a$");
        assertThat(dbHash).isNotEqualTo(newPassword);
        assertThat(passwordEncoder.matches(newPassword, dbHash)).isTrue();
        assertThat(passwordEncoder.matches(oldPassword, dbHash)).isFalse();

        // 4. Old password can no longer log in
        ResponseEntity<Map> oldLoginRes = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(username, oldPassword),
                Map.class
        );
        assertThat(oldLoginRes.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 5. New password logs in successfully
        ResponseEntity<AuthResponse> newLoginRes = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(username, newPassword),
                AuthResponse.class
        );
        assertThat(newLoginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(newLoginRes.getBody().accessToken()).isNotBlank();
    }
}
