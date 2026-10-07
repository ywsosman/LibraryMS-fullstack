package com.libraryms.error;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.libraryms.common.error.ConflictException;
import com.libraryms.common.error.ResourceNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/test-errors")
public class TestErrorController {

    public record SamplePayload(
            @NotBlank(message = "Title must not be blank")
            String title,

            @NotNull(message = "Date is required")
            String publishedDate,

            @Pattern(regexp = "^97[89][0-9]{10}$", message = "Invalid ISBN-13 format")
            String isbn
    ) {}

    @GetMapping("/not-found")
    public void throwNotFound() {
        throw new ResourceNotFoundException("TestEntity", 999L);
    }

    @GetMapping("/conflict")
    public void throwConflict() {
        throw new ConflictException("Resource already exists with key");
    }

    @GetMapping("/data-integrity")
    public void throwDataIntegrity() {
        throw new DataIntegrityViolationException("Simulated unique index violation in SQL");
    }

    @GetMapping("/optimistic-lock")
    public void throwOptimisticLock() {
        throw new ObjectOptimisticLockingFailureException("BookCopy", 1L);
    }

    @GetMapping("/access-denied")
    public void throwAccessDenied() {
        throw new AccessDeniedException("Simulated role check failure");
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String adminOnly() {
        return "admin-ok";
    }

    @GetMapping("/server-error")
    public void throwUnexpected() {
        throw new IllegalStateException("Simulated unexpected exception");
    }

    @PostMapping("/validate")
    public SamplePayload validateBody(@Valid @RequestBody SamplePayload payload) {
        return payload;
    }
}
