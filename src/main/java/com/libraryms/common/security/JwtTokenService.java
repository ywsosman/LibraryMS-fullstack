package com.libraryms.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.libraryms.user.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Generates signed JWT access tokens using Spring's oauth2-resource-server NimbusJwtEncoder,
 * and handles cryptographically secure opaque refresh tokens.
 */
@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        SecretKey secretKey = new SecretKeySpec(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    /**
     * Issues an HS256 access token. Subject is the user ID so username/email updates
     * keep active tokens valid without breaking identity.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getAccessTokenExpirationSeconds());

        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("library-ms")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("roles", roles)
                .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(() -> "HS256").build(), claims))
                .getTokenValue();
    }

    /**
     * Creates an opaque high-entropy refresh token string (256-bit secure random).
     */
    public String generateOpaqueRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Computes the SHA-256 hex digest of a refresh token to safely store in the database.
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpirationSeconds();
    }

    public long getRefreshTokenExpirationSeconds() {
        return jwtProperties.getRefreshTokenExpirationSeconds();
    }
}
