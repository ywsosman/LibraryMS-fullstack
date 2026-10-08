package com.libraryms.member.controller;

import com.libraryms.member.service.MemberService;
import com.libraryms.member.security.MemberSecurity;
import com.libraryms.member.entity.Member;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.libraryms.common.security.CurrentUserPrincipal;
import com.libraryms.member.dto.ActivateMembershipRequest;
import com.libraryms.member.dto.CreateMemberRequest;
import com.libraryms.member.dto.MemberResponse;
import com.libraryms.member.dto.UpdateMemberRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/members")
@Validated
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody CreateMemberRequest request) {
        MemberResponse created = memberService.createMember(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Page<MemberResponse>> listMembers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return ResponseEntity.ok(memberService.listMembers(name, email, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyMemberProfile(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ResponseEntity.ok(memberService.getMemberByUserId(principal.id()));
    }

    /** Any signed-in user can activate a library card for their own account. */
    @PostMapping("/me")
    public ResponseEntity<MemberResponse> activateMyMembership(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody ActivateMembershipRequest request) {
        MemberResponse created = memberService.activateMembership(principal.id(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/members/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @memberSecurity.isCurrentMember(authentication, #id)")
    public ResponseEntity<MemberResponse> getMemberById(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @memberSecurity.isCurrentMember(authentication, #id)")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequest request) {
        return ResponseEntity.ok(memberService.updateMember(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }
}
