package com.libraryms.service;

import com.libraryms.repository.UserRepository;
import com.libraryms.model.User;
import com.libraryms.mapper.UserMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

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

class UserServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private MemberRepository memberRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private UserMapper userMapper;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        memberRepository = mock(MemberRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userMapper = mock(UserMapper.class);
        userService = new UserService(userRepository, roleRepository, memberRepository, refreshTokenRepository, passwordEncoder, userMapper);
    }

    @Test
    @DisplayName("getUserById returns UserResponse when found")
    void getUserByIdReturnsResponse() {
        User user = new User("alice", "alice@example.com", "hash");
        when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
        UserResponse response = new UserResponse(1L, "alice", "alice@example.com", true, null, Set.of("ROLE_USER"), Instant.now(), Instant.now());
        when(userMapper.toResponse(user)).thenReturn(response);

        assertThat(userService.getUserById(1L)).isEqualTo(response);
    }

    @Test
    @DisplayName("getUserById throws ResourceNotFoundException when absent")
    void getUserByIdThrowsWhenNotFound() {
        when(userRepository.findWithRolesById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listUsers returns paginated responses")
    void listUsersReturnsPage() {
        User user = new User("alice", "alice@example.com", "hash");
        Page<User> page = new PageImpl<>(Collections.singletonList(user));
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        UserResponse response = new UserResponse(1L, "alice", "alice@example.com", true, null, Set.of("ROLE_USER"), Instant.now(), Instant.now());
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.listUsers("al", "al", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("updateUser rejects duplicate username or email with ConflictException")
    void updateUserRejectsDuplicates() {
        User user = new User("alice", "alice@example.com", "hash");
        when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("bob", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, new UpdateUserRequest("bob", null)))
                .isInstanceOf(ConflictException.class);

        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("bob", 1L)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("bob@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, new UpdateUserRequest("bob", "bob@example.com")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("changePassword validates current password, encodes new with BCrypt, and revokes tokens")
    void changePasswordEncodesAndRevokes() {
        User user = new User("alice", "alice@example.com", "$2a$10$oldhash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass123", "$2a$10$oldhash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequest("WrongPass123", "NewPass12345")))
                .isInstanceOf(BadRequestException.class);

        when(passwordEncoder.matches("OldPass12345", "$2a$10$oldhash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass12345")).thenReturn("$2a$10$newhash");

        userService.changePassword(1L, new ChangePasswordRequest("OldPass12345", "NewPass12345"));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$newhash");
        verify(refreshTokenRepository).revokeAllForUser(any(), any());
    }

    @Test
    @DisplayName("deleteUser removes user and revokes tokens")
    void deleteUserRemovesAndRevokes() {
        User user = new User("alice", "alice@example.com", "hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(refreshTokenRepository).revokeAllForUser(any(), any());
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("linkMember links member to user and returns response")
    void linkMemberSuccess() {
        User user = new User("alice", "alice@example.com", "hash");
        Member member = new Member("Alice Patron", "alice@example.com", null);
        UserResponse response = new UserResponse(1L, "alice", "alice@example.com", true, 10L, Set.of("ROLE_USER"), Instant.now(), Instant.now());

        when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
        when(memberRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(member));
        when(userRepository.existsByMemberIdAndIdNot(10L, 1L)).thenReturn(false);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.linkMember(1L, 10L);

        assertThat(result.memberId()).isEqualTo(10L);
        assertThat(user.getMember()).isEqualTo(member);
    }

    @Test
    @DisplayName("linkMember throws ConflictException when member already linked to another user")
    void linkMemberThrowsConflictWhenAlreadyLinked() {
        User user = new User("alice", "alice@example.com", "hash");
        Member member = new Member("Alice Patron", "alice@example.com", null);

        when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
        when(memberRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(member));
        when(userRepository.existsByMemberIdAndIdNot(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.linkMember(1L, 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already linked");
    }

    @Test
    @DisplayName("unlinkMember removes member reference from user")
    void unlinkMemberSuccess() {
        User user = new User("alice", "alice@example.com", "hash");
        Member member = new Member("Alice Patron", "alice@example.com", null);
        user.setMember(member);
        UserResponse response = new UserResponse(1L, "alice", "alice@example.com", true, null, Set.of("ROLE_USER"), Instant.now(), Instant.now());

        when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.unlinkMember(1L);

        assertThat(user.getMember()).isNull();
    }

    @Test
    @DisplayName("promoteToAdmin adds ROLE_ADMIN to user")
    void promoteToAdminSuccess() {
        User user = new User("alice", "alice@example.com", "hash");
        Role adminRole = new Role(RoleName.ROLE_ADMIN);
        UserResponse response = new UserResponse(1L, "alice", "alice@example.com", true, null, Set.of("ROLE_USER", "ROLE_ADMIN"), Instant.now(), Instant.now());

        when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.promoteToAdmin(1L);

        assertThat(user.hasRole(RoleName.ROLE_ADMIN)).isTrue();
        assertThat(result.roles()).contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("promoteToAdmin returns early if user already has ROLE_ADMIN")
    void promoteToAdminAlreadyAdmin() {
        User user = new User("alice", "alice@example.com", "hash");
        user.addRole(new Role(RoleName.ROLE_ADMIN));
        UserResponse response = new UserResponse(1L, "alice", "alice@example.com", true, null, Set.of("ROLE_ADMIN"), Instant.now(), Instant.now());

        when(userRepository.findWithRolesById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.promoteToAdmin(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("demoteFromAdmin throws BadRequestException when self-demoting")
    void demoteFromAdminRejectsSelfDemotion() {
        assertThatThrownBy(() -> userService.demoteFromAdmin(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot remove administrator privileges from your own account");
    }

    @Test
    @DisplayName("demoteFromAdmin throws ConflictException when demoting the last remaining admin")
    void demoteFromAdminRejectsLastAdmin() {
        User user = new User("alice", "alice@example.com", "hash");
        user.addRole(new Role(RoleName.ROLE_ADMIN));

        when(userRepository.findWithRolesById(2L)).thenReturn(Optional.of(user));
        when(userRepository.countByRole(RoleName.ROLE_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.demoteFromAdmin(2L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot demote the last remaining administrator");
    }

    @Test
    @DisplayName("demoteFromAdmin removes ROLE_ADMIN when multiple admins exist")
    void demoteFromAdminSuccess() {
        User user = new User("bob", "bob@example.com", "hash");
        Role adminRole = new Role(RoleName.ROLE_ADMIN);
        user.addRole(adminRole);
        UserResponse response = new UserResponse(2L, "bob", "bob@example.com", true, null, Set.of("ROLE_USER"), Instant.now(), Instant.now());

        when(userRepository.findWithRolesById(2L)).thenReturn(Optional.of(user));
        when(userRepository.countByRole(RoleName.ROLE_ADMIN)).thenReturn(3L);
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.demoteFromAdmin(2L, 1L);

        assertThat(user.hasRole(RoleName.ROLE_ADMIN)).isFalse();
        assertThat(result.roles()).doesNotContain("ROLE_ADMIN");
    }
}
