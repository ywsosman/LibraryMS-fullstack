package com.libraryms.loan;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.libraryms.common.security.CurrentUserPrincipal;
import com.libraryms.loan.dto.CreateLoanRequest;
import com.libraryms.loan.dto.LoanResponse;
import com.libraryms.loan.dto.LoanStatusFilter;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/loans")
@Validated
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_USER') and (#request.memberId == null or @memberSecurity.isCurrentMember(authentication, #request.memberId)))")
    public ResponseEntity<LoanResponse> createLoan(
            @Valid @RequestBody CreateLoanRequest request,
            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        LoanResponse created = loanService.createLoan(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LoanResponse> returnLoan(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.returnLoan(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Page<LoanResponse>> listLoans(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long copyId,
            @RequestParam(required = false) LoanStatusFilter status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(loanService.listLoans(memberId, copyId, status, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<LoanResponse>> listMyLoans(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(loanService.listMyLoans(principal.memberId(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or @loanSecurity.isLoanMember(authentication, #id)")
    public ResponseEntity<LoanResponse> getLoanById(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getLoanById(id));
    }
}
