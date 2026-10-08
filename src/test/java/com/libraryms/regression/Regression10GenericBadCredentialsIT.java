package com.libraryms.regression;

import com.libraryms.user.entity.User;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.libraryms.support.IntegrationTest;

/**
 * Regression Test 10:
 * Wrong password and unknown user return identical error responses to prevent username enumeration.
 */
@IntegrationTest
class Regression10GenericBadCredentialsIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Unknown user and wrong password return identical error status, code, and message")
    void unknownUserAndWrongPasswordReturnIdenticalResponse() {
        String knownUsername = "known_user_reg10";
        String validPassword = "CorrectPassword123";

        // Register known user
        restTemplate.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest(knownUsername, "known10@example.com", validPassword),
                AuthResponse.class
        );

        // 1. Login with unknown username
        ResponseEntity<ApiErrorResponse> unknownUserRes = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest("completely_unknown_user", "AnyPassword123"),
                ApiErrorResponse.class
        );

        // 2. Login with known username but WRONG password
        ResponseEntity<ApiErrorResponse> wrongPasswordRes = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new LoginRequest(knownUsername, "WrongPassword123"),
                ApiErrorResponse.class
        );

        // Both must return 401
        assertThat(unknownUserRes.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPasswordRes.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ApiErrorResponse body1 = unknownUserRes.getBody();
        ApiErrorResponse body2 = wrongPasswordRes.getBody();

        assertThat(body1).isNotNull();
        assertThat(body2).isNotNull();

        // Exact code and user-facing message match
        assertThat(body1.code()).isEqualTo(body2.code());
        assertThat(body1.message()).isEqualTo(body2.message());
        assertThat(body1.message()).isEqualTo("Invalid username or password");
        assertThat(body1.details()).isEqualTo(body2.details());
    }
}
