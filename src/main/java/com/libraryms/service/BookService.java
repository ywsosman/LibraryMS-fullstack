package com.libraryms.service;

import com.libraryms.mapper.BookMapper;
import com.libraryms.repository.BookRepository;
import com.libraryms.model.Book;
import com.libraryms.config.Audited;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.model.Author;
import com.libraryms.repository.AuthorRepository;
import com.libraryms.dto.BookResponse;
import com.libraryms.dto.CreateBookRequest;
import com.libraryms.dto.UpdateBookRequest;
import com.libraryms.exception.ConflictException;
import com.libraryms.exception.ResourceNotFoundException;
import com.libraryms.config.IsbnValidator;
import com.libraryms.repository.BookCopyRepository;
import com.libraryms.enums.CopyStatus;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookCopyRepository copyRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository,
                       AuthorRepository authorRepository,
                       BookCopyRepository copyRepository,
                       BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.copyRepository = copyRepository;
        this.bookMapper = bookMapper;
    }

    @Transactional
    @Audited(entityType = "BOOK", operation = "CREATE")
    public BookResponse createBook(CreateBookRequest request) {
        String normalizedIsbn = IsbnValidator.normalizeToIsbn13(request.isbn());
        if (bookRepository.existsByIsbn(normalizedIsbn)) {
            throw new ConflictException("Book with ISBN " + normalizedIsbn + " already exists");
        }

        List<Author> authors = authorRepository.findAllById(request.authorIds());
        if (authors.size() != request.authorIds().size()) {
            throw new ResourceNotFoundException("One or more author IDs were not found");
        }

        Book book = new Book(request.title().trim(), normalizedIsbn, request.publishedDate());
        book.setDescription(request.description());
        book.replaceAuthors(new HashSet<>(authors));
        if (request.genres() != null) {
            book.replaceGenres(request.genres());
        }

        Book saved = bookRepository.save(book);
        return bookMapper.toResponseWithCounts(saved, 0, 0);
    }

    @Transactional(readOnly = true)
    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));

        long totalCopies = copyRepository.countByBookId(id);
        long availableCopies = copyRepository.countByBookIdAndStatus(id, CopyStatus.AVAILABLE);

        return bookMapper.toResponseWithCounts(book, totalCopies, availableCopies);
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> listBooks(String title, String author, String isbn, String genre, Pageable pageable) {
        Specification<Book> spec = (root, query, cb) -> cb.conjunction();

        if (title != null && !title.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%"));
        }
        if (isbn != null && !isbn.isBlank()) {
            String norm = isbn.replaceAll("[-\\s]", "").trim();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isbn"), norm));
        }
        if (author != null && !author.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<Book, Author> authorJoin = root.join("authors", JoinType.INNER);
                return cb.like(cb.lower(authorJoin.get("name")), "%" + author.trim().toLowerCase() + "%");
            });
        }
        if (genre != null && !genre.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<Book, String> genreJoin = root.join("genres", JoinType.INNER);
                return cb.equal(cb.lower(genreJoin), genre.trim().toLowerCase());
            });
        }

        Page<Book> booksPage = bookRepository.findAll(spec, pageable);
        List<Book> books = booksPage.getContent();
        if (books.isEmpty()) {
            return booksPage.map(b -> bookMapper.toResponseWithCounts(b, 0, 0));
        }

        List<Long> bookIds = books.stream().map(Book::getId).toList();
        List<Object[]> countResults = copyRepository.countCopiesByBookIds(bookIds);

        Map<Long, long[]> countsByBookId = new HashMap<>();
        for (Object[] row : countResults) {
            Long bId = (Long) row[0];
            long total = ((Number) row[1]).longValue();
            long avail = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            countsByBookId.put(bId, new long[]{total, avail});
        }

        return booksPage.map(book -> {
            long[] counts = countsByBookId.getOrDefault(book.getId(), new long[]{0, 0});
            return bookMapper.toResponseWithCounts(book, counts[0], counts[1]);
        });
    }

    @Transactional
    @Audited(entityType = "BOOK", operation = "UPDATE")
    public BookResponse updateBook(Long id, UpdateBookRequest request) {
        Book book = bookRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));

        String normalizedIsbn = IsbnValidator.normalizeToIsbn13(request.isbn());
        if (!normalizedIsbn.equals(book.getIsbn()) && bookRepository.existsByIsbnAndIdNot(normalizedIsbn, id)) {
            throw new ConflictException("Book with ISBN " + normalizedIsbn + " already exists");
        }

        List<Author> authors = authorRepository.findAllById(request.authorIds());
        if (authors.size() != request.authorIds().size()) {
            throw new ResourceNotFoundException("One or more author IDs were not found");
        }

        book.setTitle(request.title().trim());
        book.setIsbn(normalizedIsbn);
        book.setPublishedDate(request.publishedDate());
        book.setDescription(request.description());
        book.replaceAuthors(new HashSet<>(authors));
        if (request.genres() != null) {
            book.replaceGenres(request.genres());
        }

        long totalCopies = copyRepository.countByBookId(id);
        long availableCopies = copyRepository.countByBookIdAndStatus(id, CopyStatus.AVAILABLE);

        return bookMapper.toResponseWithCounts(book, totalCopies, availableCopies);
    }

    @Transactional
    @Audited(entityType = "BOOK", operation = "DELETE")
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));

        if (copyRepository.existsByBookId(id)) {
            throw new ConflictException("Cannot delete book with id " + id + ": physical copies exist in library");
        }

        // Deleting book deletes join rows (book_authors, book_genres), never authors
        bookRepository.delete(book);
    }
}
