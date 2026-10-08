package com.libraryms.copy.controller;

import com.libraryms.copy.entity.CopyStatus;
import com.libraryms.copy.service.BookCopyService;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.libraryms.copy.dto.CopyResponse;
import com.libraryms.copy.dto.CreateCopyRequest;
import com.libraryms.copy.dto.UpdateCopyStatusRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/copies")
@Validated
public class BookCopyController {

    private final BookCopyService copyService;

    public BookCopyController(BookCopyService copyService) {
        this.copyService = copyService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CopyResponse> createCopy(@Valid @RequestBody CreateCopyRequest request) {
        CopyResponse created = copyService.createCopy(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CopyResponse> getCopyById(@PathVariable Long id) {
        return ResponseEntity.ok(copyService.getCopyById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CopyResponse>> listCopies(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) CopyStatus status,
            @PageableDefault(size = 20, sort = "barcode") Pageable pageable) {
        return ResponseEntity.ok(copyService.listCopies(bookId, barcode, status, pageable));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CopyResponse> updateCopyStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCopyStatusRequest request) {
        return ResponseEntity.ok(copyService.updateCopyStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteCopy(@PathVariable Long id) {
        copyService.deleteCopy(id);
        return ResponseEntity.noContent().build();
    }
}
