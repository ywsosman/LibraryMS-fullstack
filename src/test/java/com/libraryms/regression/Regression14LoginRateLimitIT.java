package com.libraryms.regression;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.libraryms.auth.dto.AuthResponse;
import com.libraryms.auth.dto.LoginRequest;
import com.libraryms.auth.dto.RegisterRequest;
import com.libraryms.common.error.ApiErrorResponse;
import com.libraryms.common.security.LoginAttemptLimiter;
import com.libraryms.support.IntegrationTest;

/**
 * Regression Test 14:
 * Login rate limit / lockout triggers after N failures (returns HTTP 429 TOO_MANY_REQUESTS).
 */
@IntegrationTest
class Regression14LoginRateLimitIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LoginAttemptLimiter loginAttemptLimiter;

    @BeforeEach
    void setUp() {
        loginAttemptLimiter.resetAll();
    }

    @Test
    @DisplayName("Login rate limit triggers 429 TOO_MANY_REQUESTS after 5 failed attempts")
    void loginRateLimitTriggersAfterFiveFailures() {
        String username = "ratelimit_target_user";
        String validPassword = "CorrectPassword123";

        // Register the user
        restTemplate.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest(username, "ratelimit@example.com", validPassword),
                AuthResponse.class
        );

        // 5 consecutive wrong password attempts -> each returns 401
        for (int i = 1; i <= LoginAttemptLimiter.MAX_FAILED_ATTEMPTS; i++) {
            ResponseEntity<ApiErrorResponse> failedRes = restTemplate.postForEntity(
                    "/api/v1/auth/login",
                    new LoginRequest(username, "WrongPasswordAttempt" + i),
                    ApiErrorResponse.class
            );
            assertThat(failedRes.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // 6th attempt (even with valid password) must trigger 429 TOO_MANY_REQUESTS
        ResponseEntity<ApiErrorResponse> lockedRes = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(username, validPassword),
                ApiErrorResponse.class
        );

        assertThat(lockedRes.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        ApiErrorResponse body = lockedRes.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("TOO_MANY_ATTEMPTS");
        assertThat(body.message()).contains("temporarily locked");
    }
}
