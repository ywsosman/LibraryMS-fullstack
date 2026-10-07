package com.libraryms.user;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.auth.RefreshTokenRepository;
import com.libraryms.common.error.BadRequestException;
import com.libraryms.common.error.ConflictException;
import com.libraryms.common.error.ResourceNotFoundException;
import com.libraryms.user.dto.ChangePasswordRequest;
import com.libraryms.user.dto.UpdateUserRequest;
import com.libraryms.user.dto.UserResponse;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String username, String email, Pageable pageable) {
        Specification<User> spec = Specification.where(null);
        if (username != null && !username.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("username")), "%" + username.trim().toLowerCase() + "%"));
        }
        if (email != null && !email.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + email.trim().toLowerCase() + "%"));
        }
        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.username() != null && !request.username().isBlank()) {
            String newUsername = request.username().trim();
            if (!newUsername.equalsIgnoreCase(user.getUsername())) {
                if (userRepository.existsByUsernameIgnoreCaseAndIdNot(newUsername, id)) {
                    throw new ConflictException("Username is already taken: " + newUsername);
                }
                user.setUsername(newUsername);
            }
        }

        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = request.email().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmailIgnoreCaseAndIdNot(newEmail, id)) {
                    throw new ConflictException("Email is already registered: " + newEmail);
                }
                user.setEmail(newEmail);
            }
        }

        return userMapper.toResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password does not match");
        }

        // Passwords are ALWAYS encoded with BCrypt before saving in every code path
        String newHash = passwordEncoder.encode(request.newPassword());
        user.setPasswordHash(newHash);

        // Invalidate active refresh tokens across all sessions on password change
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        userRepository.delete(user);
    }
}
