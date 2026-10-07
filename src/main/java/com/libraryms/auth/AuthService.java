package com.libraryms.auth;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.auth.dto.AuthResponse;
import com.libraryms.auth.dto.LoginRequest;
import com.libraryms.auth.dto.RefreshTokenRequest;
import com.libraryms.auth.dto.RegisterRequest;
import com.libraryms.common.error.ConflictException;
import com.libraryms.common.error.TooManyRequestsException;
import com.libraryms.common.error.UnauthorizedException;
import com.libraryms.common.security.JwtTokenService;
import com.libraryms.common.security.LoginAttemptLimiter;
import com.libraryms.user.Role;
import com.libraryms.user.RoleName;
import com.libraryms.user.RoleRepository;
import com.libraryms.user.User;
import com.libraryms.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final LoginAttemptLimiter loginAttemptLimiter;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       LoginAttemptLimiter loginAttemptLimiter) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.loginAttemptLimiter = loginAttemptLimiter;
    }

    public record RegistrationResult(AuthResponse authResponse, Long userId) {}

    @Transactional
    public RegistrationResult register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.username().trim(), request.email().trim().toLowerCase(), encodedPassword);

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER not found"));
        user.addRole(userRole);

        user = userRepository.save(user);

        AuthResponse authResponse = issueTokens(user);
        return new RegistrationResult(authResponse, user.getId());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String usernameKey = request.username().trim();
        if (loginAttemptLimiter.isLocked(usernameKey)) {
            throw new TooManyRequestsException(
                    "Account temporarily locked due to too many failed login attempts. Please try again later."
            );
        }

        User user = userRepository.findWithRolesByUsernameIgnoreCase(usernameKey)
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash()) || !user.isEnabled()) {
            loginAttemptLimiter.recordFailure(usernameKey);
            // Generic message for both unknown user and wrong password to prevent username enumeration
            throw new UnauthorizedException("Invalid username or password");
        }

        loginAttemptLimiter.recordSuccess(usernameKey);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String hash = jwtTokenService.hashToken(request.refreshToken());
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        Instant now = Instant.now();
        if (!token.isActive(now) || !token.getUser().isEnabled()) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        // Token rotation: revoke used token
        token.revoke(now);

        return issueTokens(token.getUser());
    }

    @Transactional
    public void logout(Long userId) {
        if (userId != null) {
            refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        }
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtTokenService.generateAccessToken(user);
        String opaqueRefresh = jwtTokenService.generateOpaqueRefreshToken();
        String refreshHash = jwtTokenService.hashToken(opaqueRefresh);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtTokenService.getRefreshTokenExpirationSeconds());

        RefreshToken refreshToken = new RefreshToken(user, refreshHash, now, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.of(accessToken, opaqueRefresh, jwtTokenService.getAccessTokenExpirationSeconds());
    }
}
