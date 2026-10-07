package com.libraryms.member;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.common.error.ConflictException;
import com.libraryms.common.error.ResourceNotFoundException;
import com.libraryms.loan.LoanRepository;
import com.libraryms.member.dto.CreateMemberRequest;
import com.libraryms.member.dto.MemberResponse;
import com.libraryms.member.dto.UpdateMemberRequest;
import com.libraryms.user.User;
import com.libraryms.user.UserRepository;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final MemberMapper memberMapper;

    public MemberService(MemberRepository memberRepository,
                         LoanRepository loanRepository,
                         UserRepository userRepository,
                         MemberMapper memberMapper) {
        this.memberRepository = memberRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.memberMapper = memberMapper;
    }

    @Transactional
    public MemberResponse createMember(CreateMemberRequest request) {
        String email = request.email().trim().toLowerCase();
        if (memberRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            throw new ConflictException("Member with email " + email + " already exists");
        }

        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        Member member = new Member(request.fullName().trim(), email, phone);
        Member saved = memberRepository.save(member);
        return memberMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMemberById(Long id) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member", id));
        return memberMapper.toResponse(member);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMemberByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (user.getMember() == null || user.getMember().isDeleted()) {
            throw new ResourceNotFoundException("Member profile for user " + user.getUsername() + " not found");
        }
        return memberMapper.toResponse(user.getMember());
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> listMembers(String name, String email, Pageable pageable) {
        Specification<Member> spec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));

        if (name != null && !name.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("fullName")), "%" + name.trim().toLowerCase() + "%"));
        }
        if (email != null && !email.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + email.trim().toLowerCase() + "%"));
        }

        return memberRepository.findAll(spec, pageable).map(memberMapper::toResponse);
    }

    @Transactional
    public MemberResponse updateMember(Long id, UpdateMemberRequest request) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member", id));

        if (request.fullName() != null && !request.fullName().isBlank()) {
            member.setFullName(request.fullName().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = request.email().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(member.getEmail())) {
                if (memberRepository.existsByEmailIgnoreCaseAndDeletedAtIsNullAndIdNot(newEmail, id)) {
                    throw new ConflictException("Email is already used by another member: " + newEmail);
                }
                member.setEmail(newEmail);
            }
        }

        if (request.phone() != null) {
            member.setPhone(request.phone().isBlank() ? null : request.phone().trim());
        }

        return memberMapper.toResponse(member);
    }

    @Transactional
    public void deleteMember(Long id) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member", id));

        if (loanRepository.existsByMemberIdAndReturnedAtIsNull(id)) {
            throw new ConflictException("Cannot delete member with active open loans");
        }

        member.markDeleted(Instant.now());
    }
}
