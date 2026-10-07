package com.libraryms.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.libraryms.user.Role;
import com.libraryms.user.RoleName;
import com.libraryms.user.User;

class JwtTokenServiceTest {

    private JwtTokenService tokenService;
    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("this-is-a-32-byte-minimum-secret-for-jwt-signing!");
        properties.setAccessTokenExpirationSeconds(900);
        properties.setRefreshTokenExpirationSeconds(604800);
        tokenService = new JwtTokenService(properties);
    }

    @Test
    @DisplayName("Generates valid non-blank access token from user")
    void generatesAccessToken() {
        User user = new User("alice", "alice@example.com", "$2a$10$7EqJtq98hPqEX7fNZaFWoO.fSjN6f6F6qX.0dZqIuA9cQ8zZ8.Hwe");
        // Reflection or helper to give user an ID since BaseEntity ID is generated on persist
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 42L);

        String token = tokenService.generateAccessToken(user);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // Header.Payload.Signature
    }

    @Test
    @DisplayName("Generates random opaque refresh token and correctly hashes it")
    void generatesAndHashesRefreshToken() {
        String raw1 = tokenService.generateOpaqueRefreshToken();
        String raw2 = tokenService.generateOpaqueRefreshToken();

        assertThat(raw1).isNotBlank();
        assertThat(raw2).isNotBlank();
        assertThat(raw1).isNotEqualTo(raw2);

        String hash1 = tokenService.hashToken(raw1);
        String hash2 = tokenService.hashToken(raw1); // same input -> same hash
        String hashDifferent = tokenService.hashToken(raw2);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(hashDifferent);
        assertThat(hash1).hasSize(64); // SHA-256 hex string is 64 characters
    }

    @Test
    @DisplayName("Exposes configured token expiration durations")
    void exposesExpirations() {
        assertThat(tokenService.getAccessTokenExpirationSeconds()).isEqualTo(900);
        assertThat(tokenService.getRefreshTokenExpirationSeconds()).isEqualTo(604800);
    }
}
