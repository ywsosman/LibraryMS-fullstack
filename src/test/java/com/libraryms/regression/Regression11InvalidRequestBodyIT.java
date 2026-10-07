package com.libraryms.regression;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
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
 * Regression Test 11:
 * Request bodies with invalid fields (blank title, null date, bad ISBN) return 400, never 500.
 * Malformed JSON payloads also return 400, never 500.
 */
@IntegrationTest
class Regression11InvalidRequestBodyIT {

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
        User user = userRepository.findByMemberId(null)
                .filter(u -> "reg11_user".equalsIgnoreCase(u.getUsername()))
                .orElseGet(() -> {
                    User u = new User(
                            "reg11_user",
                            "reg11_user@example.com",
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
    @DisplayName("Blank title returns 400 BAD_REQUEST, never 500")
    void blankTitleReturns400Never500() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "   ");
        payload.put("publishedDate", "2023-01-01");
        payload.put("isbn", "9780134685991");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/test-errors/validate",
                HttpMethod.POST,
                new HttpEntity<>(payload, authHeaders()),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().details()).anyMatch(d -> d.contains("title"));
    }

    @Test
    @DisplayName("Null published date returns 400 BAD_REQUEST, never 500")
    void nullDateReturns400Never500() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Valid Title");
        payload.put("publishedDate", null);
        payload.put("isbn", "9780134685991");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/test-errors/validate",
                HttpMethod.POST,
                new HttpEntity<>(payload, authHeaders()),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().details()).anyMatch(d -> d.contains("publishedDate"));
    }

    @Test
    @DisplayName("Malformed/invalid ISBN format returns 400 BAD_REQUEST, never 500")
    void badIsbnReturns400Never500() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Valid Title");
        payload.put("publishedDate", "2023-01-01");
        payload.put("isbn", "123-not-valid-isbn");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/test-errors/validate",
                HttpMethod.POST,
                new HttpEntity<>(payload, authHeaders()),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().details()).anyMatch(d -> d.contains("isbn"));
    }

    @Test
    @DisplayName("Malformed JSON syntax returns 400 MALFORMED_REQUEST, never 500")
    void brokenJsonReturns400Never500() {
        String brokenJson = "{\"title\": \"Unclosed JSON...";

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/test-errors/validate",
                HttpMethod.POST,
                new HttpEntity<>(brokenJson, authHeaders()),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("MALFORMED_REQUEST");
    }
}
