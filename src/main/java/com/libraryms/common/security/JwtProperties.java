package com.libraryms.common.security;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;

@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {

    /**
     * Secret key for HS256 HMAC-SHA256 signing. Must be at least 32 bytes (256 bits).
     */
    private String secret;

    /**
     * Access token lifetime in seconds (default 900 = 15 minutes).
     */
    private long accessTokenExpirationSeconds = 900;

    /**
     * Refresh token lifetime in seconds (default 604800 = 7 days).
     */
    private long refreshTokenExpirationSeconds = 604800;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public void setAccessTokenExpirationSeconds(long accessTokenExpirationSeconds) {
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    public void setRefreshTokenExpirationSeconds(long refreshTokenExpirationSeconds) {
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is missing! A secret of at least 32 bytes must be configured.");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short (" + secretBytes.length + " bytes). It must be at least 32 bytes (256 bits) for HMAC-SHA256."
            );
        }
    }
}
