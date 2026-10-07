package com.libraryms.regression;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
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

import com.libraryms.common.error.ApiErrorResponse;
import com.libraryms.common.security.JwtTokenService;
import com.libraryms.support.IntegrationTest;
import com.libraryms.user.Role;
import com.libraryms.user.RoleName;
import com.libraryms.user.RoleRepository;
import com.libraryms.user.User;
import com.libraryms.user.UserRepository;

/**
 * Regression Test 9:
 * Verifies exact status mappings:
 * Not-found -> 404, duplicate/conflict -> 409, forbidden -> 403, validation -> 400 with field errors.
 */
@IntegrationTest
class Regression09ErrorStatusMappingIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String userToken;

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();
        User user = userRepository.findWithRolesByUsernameIgnoreCase("reg9_user")
                .orElseGet(() -> {
                    User u = new User(
                            "reg9_user",
                            "reg9_user@example.com",
                            "$2a$10$7EqJtq98hPqEX7fNZaFWoO.fSjN6f6F6qX.0dZqIuA9cQ8zZ8.Hwe"
                    );
                    u.addRole(userRole);
                    return userRepository.saveAndFlush(u);
                });

        this.userToken = jwtTokenService.generateAccessToken(user);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @DisplayName("Not found maps to 404 with standard body")
    void notFoundMapsTo404() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/test-errors/not-found",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(body.message()).contains("TestEntity");
    }

    @Test
    @DisplayName("Conflict and data integrity map to 409 with standard body")
    void conflictMapsTo409() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/test-errors/conflict",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("CONFLICT");

        // Also test data integrity violation mapping
        ResponseEntity<ApiErrorResponse> diResponse = restTemplate.exchange(
                "/api/v1/test-errors/data-integrity",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                ApiErrorResponse.class
        );

        assertThat(diResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(diResponse.getBody().code()).isEqualTo("CONFLICT");
    }

    @Test
    @DisplayName("Forbidden maps to 403 when USER role accesses ADMIN endpoint")
    void forbiddenMapsTo403() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/test-errors/admin-only",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("Validation failure maps to 400 with field-level errors list")
    void validationMapsTo400WithFieldErrors() {
        // Empty payload violating all validation constraints
        Map<String, String> invalidPayload = Map.of(
                "title", "   ",
                "isbn", "invalid-isbn"
        );

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/test-errors/validate",
                HttpMethod.POST,
                new HttpEntity<>(invalidPayload, authHeaders()),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.details()).isNotEmpty();
        assertThat(body.details()).anyMatch(d -> d.contains("title"));
        assertThat(body.details()).anyMatch(d -> d.contains("publishedDate"));
        assertThat(body.details()).anyMatch(d -> d.contains("isbn"));
    }
}
