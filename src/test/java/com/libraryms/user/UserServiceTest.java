package com.libraryms.user;

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

import com.libraryms.auth.RefreshTokenRepository;
import com.libraryms.common.error.BadRequestException;
import com.libraryms.common.error.ConflictException;
import com.libraryms.common.error.ResourceNotFoundException;
import com.libraryms.member.Member;
import com.libraryms.member.MemberRepository;
import com.libraryms.user.dto.ChangePasswordRequest;
import com.libraryms.user.dto.UpdateUserRequest;
import com.libraryms.user.dto.UserResponse;

class UserServiceTest {

    private UserRepository userRepository;
    private MemberRepository memberRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private UserMapper userMapper;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        memberRepository = mock(MemberRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userMapper = mock(UserMapper.class);
        userService = new UserService(userRepository, memberRepository, refreshTokenRepository, passwordEncoder, userMapper);
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
}
