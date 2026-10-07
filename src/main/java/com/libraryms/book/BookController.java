package com.libraryms.book;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.libraryms.book.dto.BookResponse;
import com.libraryms.book.dto.CreateBookRequest;
import com.libraryms.book.dto.UpdateBookRequest;
import com.libraryms.copy.BookCopyService;
import com.libraryms.copy.dto.CopyResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/books")
@Validated
public class BookController {

    private final BookService bookService;
    private final BookCopyService copyService;

    public BookController(BookService bookService, BookCopyService copyService) {
        this.bookService = bookService;
        this.copyService = copyService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookResponse created = bookService.createBook(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/{id}/copies")
    public ResponseEntity<Page<CopyResponse>> getBookCopies(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        bookService.getBookById(id);
        return ResponseEntity.ok(copyService.listCopies(id, null, null, pageable));
    }

    @GetMapping
    public ResponseEntity<Page<BookResponse>> listBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String genre,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookService.listBooks(title, author, isbn, genre, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookResponse> updateBook(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateBookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
