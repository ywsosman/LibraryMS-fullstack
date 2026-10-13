package com.libraryms.service;

import com.libraryms.model.Author;
import com.libraryms.mapper.AuthorMapper;
import com.libraryms.config.Audited;
import com.libraryms.repository.AuthorRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.dto.AuthorResponse;
import com.libraryms.dto.CreateAuthorRequest;
import com.libraryms.dto.UpdateAuthorRequest;
import com.libraryms.exception.ConflictException;
import com.libraryms.exception.ResourceNotFoundException;
import com.libraryms.model.Book;
import com.libraryms.repository.BookRepository;

import java.util.List;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final AuthorMapper authorMapper;

    public AuthorService(AuthorRepository authorRepository, BookRepository bookRepository, AuthorMapper authorMapper) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.authorMapper = authorMapper;
    }

    @Transactional
    @Audited(entityType = "AUTHOR", operation = "CREATE")
    public AuthorResponse createAuthor(CreateAuthorRequest request) {
        Author author = authorMapper.toEntity(request);
        Author saved = authorRepository.save(author);
        return authorMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthorResponse getAuthorById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));
        return authorMapper.toResponse(author);
    }

    @Transactional(readOnly = true)
    public Page<AuthorResponse> listAuthors(String name, Pageable pageable) {
        Specification<Author> spec = (root, query, cb) -> cb.conjunction();
        if (name != null && !name.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
        }
        return authorRepository.findAll(spec, pageable).map(authorMapper::toResponse);
    }

    @Transactional
    @Audited(entityType = "AUTHOR", operation = "UPDATE")
    public AuthorResponse updateAuthor(Long id, UpdateAuthorRequest request) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));
        authorMapper.updateEntity(request, author);
        return authorMapper.toResponse(author);
    }

    @Transactional
    @Audited(entityType = "AUTHOR", operation = "DELETE")
    public void deleteAuthor(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));

        List<Book> soleAuthorBooks = bookRepository.findBooksWhereSoleAuthor(id);
        if (!soleAuthorBooks.isEmpty()) {
            throw new ConflictException("Cannot delete author because book '" + soleAuthorBooks.get(0).getTitle() + "' would be left with no authors");
        }

        authorRepository.delete(author);
    }
}
