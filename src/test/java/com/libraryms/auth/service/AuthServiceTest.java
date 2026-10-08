package com.libraryms.auth.service;

import com.libraryms.audit.service.AuditService;
import com.libraryms.auth.entity.RefreshToken;
import com.libraryms.auth.repository.RefreshTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.libraryms.auth.dto.AuthResponse;
import com.libraryms.auth.dto.LoginRequest;
import com.libraryms.auth.dto.RefreshTokenRequest;
import com.libraryms.auth.dto.RegisterRequest;
import com.libraryms.common.error.ConflictException;
import com.libraryms.common.error.TooManyRequestsException;
import com.libraryms.common.error.UnauthorizedException;
import com.libraryms.common.security.JwtTokenService;
import com.libraryms.common.security.LoginAttemptLimiter;
import com.libraryms.user.entity.Role;
import com.libraryms.user.entity.RoleName;
import com.libraryms.user.repository.RoleRepository;
import com.libraryms.user.entity.User;
import com.libraryms.user.repository.UserRepository;

class AuthServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenService jwtTokenService;
    private LoginAttemptLimiter loginAttemptLimiter;
    private com.libraryms.audit.service.AuditService auditService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenService = mock(JwtTokenService.class);
        loginAttemptLimiter = mock(LoginAttemptLimiter.class);
        auditService = mock(com.libraryms.audit.service.AuditService.class);

        authService = new AuthService(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenService,
                loginAttemptLimiter,
                auditService
        );
    }

    @Test
    @DisplayName("Register rejects duplicate username with ConflictException")
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "alice@example.com", "Password12345")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username is already taken");
    }

    @Test
    @DisplayName("Register rejects duplicate email with ConflictException")
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "alice@example.com", "Password12345")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is already registered");
    }

    @Test
    @DisplayName("Register persists user and returns tokens")
    void registerSucceeds() {
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password12345")).thenReturn("$2a$10$encodedhash");

        Role role = mock(Role.class);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(role));

        User savedUser = new User("alice", "alice@example.com", "$2a$10$encodedhash");
        org.springframework.test.util.ReflectionTestUtils.setField(savedUser, "id", 1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(jwtTokenService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtTokenService.generateOpaqueRefreshToken()).thenReturn("refresh-token");
        when(jwtTokenService.hashToken("refresh-token")).thenReturn("refresh-hash");
        when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthService.RegistrationResult result = authService.register(
                new RegisterRequest("alice", "alice@example.com", "Password12345")
        );

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.authResponse().accessToken()).isEqualTo("access-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Login rejects locked accounts with TooManyRequestsException")
    void loginRejectsLockedAccounts() {
        when(loginAttemptLimiter.isLocked("lockeduser")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("lockeduser", "Password123")))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("Login rejects bad credentials with UnauthorizedException and records failure")
    void loginRejectsBadCredentials() {
        when(loginAttemptLimiter.isLocked("alice")).thenReturn(false);
        when(userRepository.findWithRolesByUsernameIgnoreCase("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "WrongPassword")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid username or password");

        verify(loginAttemptLimiter).recordFailure("alice");
    }

    @Test
    @DisplayName("Login succeeds with valid credentials and clears rate limiter")
    void loginSucceeds() {
        User user = new User("alice", "alice@example.com", "$2a$10$encoded");
        when(loginAttemptLimiter.isLocked("alice")).thenReturn(false);
        when(userRepository.findWithRolesByUsernameIgnoreCase("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("GoodPassword123", "$2a$10$encoded")).thenReturn(true);

        when(jwtTokenService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtTokenService.generateOpaqueRefreshToken()).thenReturn("refresh-token");
        when(jwtTokenService.hashToken("refresh-token")).thenReturn("hash");

        AuthResponse response = authService.login(new LoginRequest("alice", "GoodPassword123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(loginAttemptLimiter).recordSuccess("alice");
    }

    @Test
    @DisplayName("Refresh rotates tokens successfully")
    void refreshSucceeds() {
        User user = new User("alice", "alice@example.com", "hash");
        RefreshToken token = new RefreshToken(user, "hash", Instant.now().minusSeconds(100), Instant.now().plusSeconds(1000));

        when(jwtTokenService.hashToken("valid-refresh")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));
        when(jwtTokenService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtTokenService.generateOpaqueRefreshToken()).thenReturn("new-refresh");
        when(jwtTokenService.hashToken("new-refresh")).thenReturn("new-hash");

        AuthResponse response = authService.refresh(new RefreshTokenRequest("valid-refresh"));
        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(token.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("Logout revokes all user refresh tokens")
    void logoutRevokesTokens() {
        authService.logout(5L);
        verify(refreshTokenRepository).revokeAllForUser(any(), any());
    }
}
