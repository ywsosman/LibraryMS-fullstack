package com.libraryms.book.service;

import com.libraryms.book.mapper.BookMapper;
import com.libraryms.book.repository.BookRepository;
import com.libraryms.book.entity.Book;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

import com.libraryms.author.entity.Author;
import com.libraryms.author.repository.AuthorRepository;
import com.libraryms.book.dto.BookResponse;
import com.libraryms.book.dto.CreateBookRequest;
import com.libraryms.book.dto.UpdateBookRequest;
import com.libraryms.common.dto.summary.AuthorSummary;
import com.libraryms.common.error.ConflictException;
import com.libraryms.common.error.ResourceNotFoundException;
import com.libraryms.copy.repository.BookCopyRepository;
import com.libraryms.copy.entity.CopyStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookCopyRepository copyRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookService bookService;

    @Test
    @DisplayName("createBook should normalize ISBN, link authors, save and return response")
    void createBook_success() {
        CreateBookRequest request = new CreateBookRequest(
                "Animal Farm",
                "978-0-452-28424-1",
                LocalDate.of(1945, 8, 17),
                "Satire",
                Set.of(1L),
                Set.of("Classics", "Dystopian")
        );
        Author author = new Author("George Orwell", "Bio", null);
        Book book = new Book("Animal Farm", "9780452284241", LocalDate.of(1945, 8, 17));
        BookResponse response = new BookResponse(
                1L, "Animal Farm", "9780452284241", LocalDate.of(1945, 8, 17),
                "Satire", Set.of(new AuthorSummary(1L, "George Orwell")), Set.of("Classics", "Dystopian"),
                0, 0, null, null
        );

        when(bookRepository.existsByIsbn("9780452284241")).thenReturn(false);
        when(authorRepository.findAllById(Set.of(1L))).thenReturn(List.of(author));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(bookMapper.toResponseWithCounts(book, 0, 0)).thenReturn(response);

        BookResponse result = bookService.createBook(request);

        assertThat(result).isNotNull();
        assertThat(result.isbn()).isEqualTo("9780452284241");
    }

    @Test
    @DisplayName("createBook should throw ConflictException if ISBN already exists")
    void createBook_duplicateIsbn_throwsConflict() {
        CreateBookRequest request = new CreateBookRequest(
                "Animal Farm",
                "978-0-452-28424-1",
                null, null, Set.of(1L), Set.of()
        );
        when(bookRepository.existsByIsbn("9780452284241")).thenReturn(true);

        assertThatThrownBy(() -> bookService.createBook(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("createBook should throw ResourceNotFoundException if author IDs do not exist")
    void createBook_missingAuthor_throwsNotFound() {
        CreateBookRequest request = new CreateBookRequest(
                "Animal Farm",
                "978-0-452-28424-1",
                null, null, Set.of(1L, 2L), Set.of()
        );
        when(bookRepository.existsByIsbn("9780452284241")).thenReturn(false);
        when(authorRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(new Author("A", "B", null)));

        assertThatThrownBy(() -> bookService.createBook(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("author IDs");
    }

    @Test
    @DisplayName("getBookById should return book with copy counts")
    void getBookById_found() {
        Book book = new Book("Animal Farm", "9780452284241", null);
        BookResponse response = new BookResponse(
                1L, "Animal Farm", "9780452284241", null, null, Set.of(), Set.of(), 5, 3, null, null
        );

        when(bookRepository.findDetailedById(1L)).thenReturn(Optional.of(book));
        when(copyRepository.countByBookId(1L)).thenReturn(5L);
        when(copyRepository.countByBookIdAndStatus(1L, CopyStatus.AVAILABLE)).thenReturn(3L);
        when(bookMapper.toResponseWithCounts(book, 5L, 3L)).thenReturn(response);

        BookResponse result = bookService.getBookById(1L);

        assertThat(result.totalCopies()).isEqualTo(5);
        assertThat(result.availableCopies()).isEqualTo(3);
    }

    @Test
    @DisplayName("getBookById should throw ResourceNotFoundException if not found")
    void getBookById_notFound() {
        when(bookRepository.findDetailedById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listBooks should filter and return page with copy counts")
    void listBooks_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Book book = new Book("Animal Farm", "9780452284241", null);
        Page<Book> pagedBooks = new PageImpl<>(List.of(book), pageable, 1);
        BookResponse response = new BookResponse(
                1L, "Animal Farm", "9780452284241", null, null, Set.of(), Set.of(), 2, 2, null, null
        );

        when(bookRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(pagedBooks);
        when(copyRepository.countByBookId(any())).thenReturn(2L);
        when(copyRepository.countByBookIdAndStatus(any(), eq(CopyStatus.AVAILABLE))).thenReturn(2L);
        when(bookMapper.toResponseWithCounts(book, 2L, 2L)).thenReturn(response);

        Page<BookResponse> result = bookService.listBooks("Animal", "Orwell", "9780452284241", "Classics", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("updateBook should update book and return updated response")
    void updateBook_success() {
        UpdateBookRequest request = new UpdateBookRequest(
                "Nineteen Eighty-Four",
                "978-0-452-28423-4",
                LocalDate.of(1949, 6, 8),
                "Dystopian novel",
                Set.of(1L),
                Set.of("Sci-Fi")
        );
        Book book = new Book("Old Title", "9780452284241", null);
        Author author = new Author("George Orwell", "Bio", null);
        BookResponse response = new BookResponse(
                1L, "Nineteen Eighty-Four", "9780452284234", LocalDate.of(1949, 6, 8),
                "Dystopian novel", Set.of(), Set.of("Sci-Fi"), 1, 1, null, null
        );

        when(bookRepository.findDetailedById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.existsByIsbnAndIdNot("9780452284234", 1L)).thenReturn(false);
        when(authorRepository.findAllById(Set.of(1L))).thenReturn(List.of(author));
        when(copyRepository.countByBookId(1L)).thenReturn(1L);
        when(copyRepository.countByBookIdAndStatus(1L, CopyStatus.AVAILABLE)).thenReturn(1L);
        when(bookMapper.toResponseWithCounts(book, 1L, 1L)).thenReturn(response);

        BookResponse result = bookService.updateBook(1L, request);

        assertThat(result.title()).isEqualTo("Nineteen Eighty-Four");
        assertThat(result.isbn()).isEqualTo("9780452284234");
    }

    @Test
    @DisplayName("updateBook should throw ConflictException if updated ISBN taken by another book")
    void updateBook_duplicateIsbn_throwsConflict() {
        UpdateBookRequest request = new UpdateBookRequest(
                "Title", "978-0-452-28423-4", null, null, Set.of(1L), Set.of()
        );
        Book book = new Book("Title", "9780452284241", null);

        when(bookRepository.findDetailedById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.existsByIsbnAndIdNot("9780452284234", 1L)).thenReturn(true);

        assertThatThrownBy(() -> bookService.updateBook(1L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("updateBook should throw ResourceNotFoundException if author missing")
    void updateBook_missingAuthor_throwsNotFound() {
        UpdateBookRequest request = new UpdateBookRequest(
                "Title", "978-0-452-28424-1", null, null, Set.of(1L, 2L), Set.of()
        );
        Book book = new Book("Title", "9780452284241", null);

        when(bookRepository.findDetailedById(1L)).thenReturn(Optional.of(book));
        when(authorRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(new Author("A", "B", null)));

        assertThatThrownBy(() -> bookService.updateBook(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteBook should delete book if no copies exist")
    void deleteBook_success() {
        Book book = new Book("Title", "9780452284241", null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(copyRepository.existsByBookId(1L)).thenReturn(false);

        bookService.deleteBook(1L);

        verify(bookRepository).delete(book);
    }

    @Test
    @DisplayName("deleteBook should throw ConflictException if physical copies exist")
    void deleteBook_withCopies_throwsConflict() {
        Book book = new Book("Title", "9780452284241", null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(copyRepository.existsByBookId(1L)).thenReturn(true);

        assertThatThrownBy(() -> bookService.deleteBook(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("physical copies exist");
    }

    @Test
    @DisplayName("deleteBook should throw ResourceNotFoundException if book not found")
    void deleteBook_notFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.deleteBook(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
