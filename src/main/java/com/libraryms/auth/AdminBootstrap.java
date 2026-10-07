package com.libraryms.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.user.Role;
import com.libraryms.user.RoleName;
import com.libraryms.user.RoleRepository;
import com.libraryms.user.User;
import com.libraryms.user.UserRepository;

/**
 * Bootstraps the initial admin account at startup ONLY IF configured via environment variables
 * (ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD) and no admin user currently exists.
 * Skips silently if environment variables are not provided.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.username:}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    public AdminBootstrap(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminUsername == null || adminUsername.isBlank()
                || adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.info("Admin bootstrap variables not set; skipping automatic admin creation.");
            return;
        }

        if (userRepository.existsByRole(RoleName.ROLE_ADMIN)) {
            log.info("Admin user already exists; skipping bootstrap.");
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found in database"));
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found in database"));

        String encodedPassword = passwordEncoder.encode(adminPassword);
        User admin = new User(adminUsername.trim(), adminEmail.trim(), encodedPassword);
        admin.addRole(adminRole);
        admin.addRole(userRole);

        userRepository.save(admin);
        log.info("Successfully bootstrapped initial admin account: {}", adminUsername.trim());
    }
}
