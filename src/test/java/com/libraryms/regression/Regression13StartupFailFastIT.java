package com.libraryms.regression;

import com.libraryms.user.entity.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.libraryms.common.security.JwtProperties;
import com.libraryms.support.IntegrationTest;
import com.libraryms.user.entity.RoleName;
import com.libraryms.user.repository.UserRepository;

/**
 * Regression Test 13:
 * 1. Startup fails fast if JWT_SECRET is missing or shorter than 32 bytes (256 bits).
 * 2. No default admin exists without ADMIN_* environment variables configured.
 */
@IntegrationTest
class Regression13StartupFailFastIT {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("No admin exists without explicit ADMIN_* bootstrap variables")
    void noDefaultAdminExistsWithoutEnvVars() {
        // Without ADMIN_* configured, zero admin accounts must exist in the database
        boolean adminExists = userRepository.existsByRole(RoleName.ROLE_ADMIN);
        assertThat(adminExists).isFalse();
    }

    @Test
    @DisplayName("JwtProperties fails fast when secret is null or blank")
    void failsFastWhenSecretIsMissing() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(null);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is missing");

        properties.setSecret("   ");
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is missing");
    }

    @Test
    @DisplayName("JwtProperties fails fast when secret is shorter than 32 bytes")
    void failsFastWhenSecretIsTooShort() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("short-secret-only-23-bytes"); // 26 bytes

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short")
                .hasMessageContaining("at least 32 bytes");
    }
}
