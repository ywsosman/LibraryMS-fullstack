package com.libraryms.service;

import com.libraryms.repository.UserRepository;
import com.libraryms.model.User;
import com.libraryms.mapper.UserMapper;
import com.libraryms.config.Audited;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.enums.RoleName;
import com.libraryms.model.Role;
import com.libraryms.repository.RoleRepository;
import com.libraryms.repository.RefreshTokenRepository;
import com.libraryms.exception.BadRequestException;
import com.libraryms.exception.ConflictException;
import com.libraryms.exception.ResourceNotFoundException;
import com.libraryms.model.Member;
import com.libraryms.repository.MemberRepository;
import com.libraryms.dto.ChangePasswordRequest;
import com.libraryms.dto.UpdateUserRequest;
import com.libraryms.dto.UserResponse;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       MemberRepository memberRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.memberRepository = memberRepository;
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
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
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
    @Audited(entityType = "USER", operation = "UPDATE")
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
    @Audited(entityType = "USER", operation = "CHANGE_PASSWORD")
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
    @Audited(entityType = "USER", operation = "DELETE")
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        userRepository.delete(user);
    }

    @Transactional
    @Audited(entityType = "USER", operation = "LINK_MEMBER")
    public UserResponse linkMember(Long userId, Long memberId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        if (userRepository.existsByMemberIdAndIdNot(memberId, userId)) {
            throw new ConflictException("Member " + memberId + " is already linked to another user");
        }

        user.setMember(member);
        return userMapper.toResponse(user);
    }

    @Transactional
    @Audited(entityType = "USER", operation = "UNLINK_MEMBER")
    public UserResponse unlinkMember(Long userId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setMember(null);
        return userMapper.toResponse(user);
    }

    @Transactional
    @Audited(entityType = "USER", operation = "PROMOTE_ADMIN")
    public UserResponse promoteToAdmin(Long userId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.hasRole(RoleName.ROLE_ADMIN)) {
            return userMapper.toResponse(user);
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found"));
        user.addRole(adminRole);
        return userMapper.toResponse(user);
    }

    @Transactional
    @Audited(entityType = "USER", operation = "DEMOTE_ADMIN")
    public UserResponse demoteFromAdmin(Long userId, Long currentUserId) {
        if (userId.equals(currentUserId)) {
            throw new BadRequestException("You cannot remove administrator privileges from your own account");
        }

        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.hasRole(RoleName.ROLE_ADMIN)) {
            return userMapper.toResponse(user);
        }

        if (userRepository.countByRole(RoleName.ROLE_ADMIN) <= 1) {
            throw new ConflictException("Cannot demote the last remaining administrator in the system");
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found"));
        user.removeRole(adminRole);
        return userMapper.toResponse(user);
    }
}
