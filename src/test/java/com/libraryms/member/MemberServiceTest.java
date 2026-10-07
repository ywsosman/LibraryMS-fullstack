package com.libraryms.member;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.libraryms.common.error.ConflictException;
import com.libraryms.common.error.ResourceNotFoundException;
import com.libraryms.loan.LoanRepository;
import com.libraryms.member.dto.CreateMemberRequest;
import com.libraryms.member.dto.MemberResponse;
import com.libraryms.member.dto.UpdateMemberRequest;
import com.libraryms.user.User;
import com.libraryms.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemberMapper memberMapper;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("createMember should save member and return response")
    void createMember_success() {
        CreateMemberRequest request = new CreateMemberRequest("Alice Smith", "alice@example.com", "1234567890");
        Member member = new Member("Alice Smith", "alice@example.com", "1234567890");
        MemberResponse response = new MemberResponse(1L, "Alice Smith", "alice@example.com", "1234567890", null, null);

        when(memberRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("alice@example.com")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(member);
        when(memberMapper.toResponse(member)).thenReturn(response);

        MemberResponse result = memberService.createMember(request);

        assertThat(result.fullName()).isEqualTo("Alice Smith");
        assertThat(result.email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("createMember should throw ConflictException if active member with email exists")
    void createMember_duplicateEmail_throwsConflict() {
        CreateMemberRequest request = new CreateMemberRequest("Alice Smith", "alice@example.com", null);
        when(memberRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.createMember(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("getMemberById should return member response if found")
    void getMemberById_found() {
        Member member = new Member("Alice Smith", "alice@example.com", null);
        MemberResponse response = new MemberResponse(1L, "Alice Smith", "alice@example.com", null, null, null);

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        when(memberMapper.toResponse(member)).thenReturn(response);

        MemberResponse result = memberService.getMemberById(1L);

        assertThat(result.fullName()).isEqualTo("Alice Smith");
    }

    @Test
    @DisplayName("getMemberById should throw ResourceNotFoundException if member not found")
    void getMemberById_notFound() {
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMemberById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getMemberByUserId should return member if linked")
    void getMemberByUserId_found() {
        User user = new User("alice", "alice@example.com", "$2a$10$...");
        Member member = new Member("Alice Smith", "alice@example.com", null);
        user.setMember(member);
        MemberResponse response = new MemberResponse(1L, "Alice Smith", "alice@example.com", null, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(memberMapper.toResponse(member)).thenReturn(response);

        MemberResponse result = memberService.getMemberByUserId(1L);

        assertThat(result.fullName()).isEqualTo("Alice Smith");
    }

    @Test
    @DisplayName("getMemberByUserId should throw ResourceNotFoundException if user has no member")
    void getMemberByUserId_noMemberLinked_throwsNotFound() {
        User user = new User("alice", "alice@example.com", "$2a$10$...");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> memberService.getMemberByUserId(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member profile for user");
    }

    @Test
    @DisplayName("listMembers should return paged active members")
    void listMembers_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Member member = new Member("Alice Smith", "alice@example.com", null);
        MemberResponse response = new MemberResponse(1L, "Alice Smith", "alice@example.com", null, null, null);
        Page<Member> page = new PageImpl<>(List.of(member), pageable, 1);

        when(memberRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(memberMapper.toResponse(member)).thenReturn(response);

        Page<MemberResponse> result = memberService.listMembers("Alice", "alice@example.com", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("updateMember should update name, email, phone")
    void updateMember_success() {
        Member member = new Member("Alice Smith", "alice@example.com", "111");
        UpdateMemberRequest request = new UpdateMemberRequest("Alice Johnson", "new@example.com", "222");
        MemberResponse response = new MemberResponse(1L, "Alice Johnson", "new@example.com", "222", null, null);

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByEmailIgnoreCaseAndDeletedAtIsNullAndIdNot("new@example.com", 1L)).thenReturn(false);
        when(memberMapper.toResponse(member)).thenReturn(response);

        MemberResponse result = memberService.updateMember(1L, request);

        assertThat(result.fullName()).isEqualTo("Alice Johnson");
        assertThat(member.getFullName()).isEqualTo("Alice Johnson");
        assertThat(member.getEmail()).isEqualTo("new@example.com");
        assertThat(member.getPhone()).isEqualTo("222");
    }

    @Test
    @DisplayName("updateMember should throw ConflictException if new email is taken")
    void updateMember_duplicateEmail_throwsConflict() {
        Member member = new Member("Alice Smith", "alice@example.com", null);
        UpdateMemberRequest request = new UpdateMemberRequest(null, "taken@example.com", null);

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByEmailIgnoreCaseAndDeletedAtIsNullAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> memberService.updateMember(1L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("deleteMember should soft delete member when no open loans")
    void deleteMember_success() {
        Member member = new Member("Alice Smith", "alice@example.com", null);

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        when(loanRepository.existsByMemberIdAndReturnedAtIsNull(1L)).thenReturn(false);

        memberService.deleteMember(1L);

        assertThat(member.isDeleted()).isTrue();
        assertThat(member.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("deleteMember should throw ConflictException when member has open loans")
    void deleteMember_withOpenLoans_throwsConflict() {
        Member member = new Member("Alice Smith", "alice@example.com", null);

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        when(loanRepository.existsByMemberIdAndReturnedAtIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> memberService.deleteMember(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active open loans");

        assertThat(member.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("deleteMember should throw ResourceNotFoundException if member not found")
    void deleteMember_notFound() {
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.deleteMember(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
