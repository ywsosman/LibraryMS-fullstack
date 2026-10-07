package com.libraryms.regression;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.libraryms.auth.dto.AuthResponse;
import com.libraryms.auth.dto.RegisterRequest;
import com.libraryms.common.error.ApiErrorResponse;
import com.libraryms.support.IntegrationTest;
import com.libraryms.user.dto.UpdateUserRequest;
import com.libraryms.user.dto.UserResponse;

/**
 * Regression Test 4:
 * Changing username or email keeps the active session valid (subject is numeric user ID),
 * and rejects conflicting duplicate usernames/emails with 409.
 */
@IntegrationTest
class Regression04UsernameEmailUpdateIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Changing username keeps active token valid and rejects duplicates with 409")
    void usernameEmailUpdateKeepsSessionValidAndRejectsDuplicates() {
        // 1. Register User A and User B
        ResponseEntity<AuthResponse> userARes = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest("user_alpha", "alpha@example.com", "PasswordAlpha123"),
                AuthResponse.class
        );
        assertThat(userARes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String tokenA = userARes.getBody().accessToken();

        ResponseEntity<AuthResponse> userBRes = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest("user_beta", "beta@example.com", "PasswordBeta123"),
                AuthResponse.class
        );
        assertThat(userBRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);
        headersA.setContentType(MediaType.APPLICATION_JSON);

        // 2. User A attempts to take User B's username -> 409 CONFLICT
        ResponseEntity<ApiErrorResponse> dupUserRes = restTemplate.exchange(
                "/api/v1/users/me",
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateUserRequest("user_beta", null), headersA),
                ApiErrorResponse.class
        );
        assertThat(dupUserRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(dupUserRes.getBody().code()).isEqualTo("CONFLICT");

        // 3. User A attempts to take User B's email -> 409 CONFLICT
        ResponseEntity<ApiErrorResponse> dupEmailRes = restTemplate.exchange(
                "/api/v1/users/me",
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateUserRequest(null, "beta@example.com"), headersA),
                ApiErrorResponse.class
        );
        assertThat(dupEmailRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(dupEmailRes.getBody().code()).isEqualTo("CONFLICT");

        // 4. User A updates username and email to new valid values -> 200 OK
        ResponseEntity<UserResponse> updateOkRes = restTemplate.exchange(
                "/api/v1/users/me",
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateUserRequest("user_alpha_renamed", "alpha_renamed@example.com"), headersA),
                UserResponse.class
        );
        assertThat(updateOkRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateOkRes.getBody().username()).isEqualTo("user_alpha_renamed");
        assertThat(updateOkRes.getBody().email()).isEqualTo("alpha_renamed@example.com");

        // 5. User A calls GET /api/v1/users/me with the SAME original token -> 200 OK (session stays valid)
        ResponseEntity<UserResponse> meRes = restTemplate.exchange(
                "/api/v1/users/me",
                HttpMethod.GET,
                new HttpEntity<>(headersA),
                UserResponse.class
        );
        assertThat(meRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meRes.getBody().username()).isEqualTo("user_alpha_renamed");
    }
}
