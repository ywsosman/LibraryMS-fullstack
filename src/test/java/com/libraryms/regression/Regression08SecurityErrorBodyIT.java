package com.libraryms.regression;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

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
import com.libraryms.common.security.JwtProperties;
import com.libraryms.support.IntegrationTest;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Regression Test 8:
 * Invalid/expired/missing token returns JSON 401 with the standard error body:
 * { code, message, details[], timestamp, path } and application/json Content-Type.
 */
@IntegrationTest
class Regression08SecurityErrorBodyIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtProperties jwtProperties;

    private static final String PROTECTED_URL = "/api/v1/test-errors/not-found";

    @Test
    @DisplayName("Missing Authorization header returns 401 with standard error JSON body")
    void missingTokenReturns401WithStandardBody() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                PROTECTED_URL, ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON)).isTrue();

        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("UNAUTHORIZED");
        assertThat(body.message()).isNotEmpty();
        assertThat(body.details()).isNotNull();
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.path()).isEqualTo(PROTECTED_URL);
    }

    @Test
    @DisplayName("Invalid token signature/format returns 401 with standard error JSON body")
    void invalidTokenReturns401WithStandardBody() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid.malformed.jwttokenvalue");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                PROTECTED_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON)).isTrue();

        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("UNAUTHORIZED");
        assertThat(body.path()).isEqualTo(PROTECTED_URL);
    }

    @Test
    @DisplayName("Expired token returns 401 with standard error JSON body")
    void expiredTokenReturns401WithStandardBody() throws Exception {
        // Sign an expired token using the active JWT secret
        SecretKey key = new SecretKeySpec(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("1")
                .issuer("library-ms")
                .issueTime(new Date(System.currentTimeMillis() - 3600_000))
                .expirationTime(new Date(System.currentTimeMillis() - 1800_000)) // expired 30 mins ago
                .claim("roles", List.of("ROLE_USER"))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(new MACSigner(key));
        String expiredToken = signedJWT.serialize();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expiredToken);

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                PROTECTED_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON)).isTrue();

        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("UNAUTHORIZED");
        assertThat(body.path()).isEqualTo(PROTECTED_URL);
    }
}
