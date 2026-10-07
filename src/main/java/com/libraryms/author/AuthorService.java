package com.libraryms.author;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.author.dto.AuthorResponse;
import com.libraryms.author.dto.CreateAuthorRequest;
import com.libraryms.author.dto.UpdateAuthorRequest;
import com.libraryms.common.error.ResourceNotFoundException;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;

    public AuthorService(AuthorRepository authorRepository, AuthorMapper authorMapper) {
        this.authorRepository = authorRepository;
        this.authorMapper = authorMapper;
    }

    @Transactional
    @com.libraryms.audit.Audited(entityType = "AUTHOR", operation = "CREATE")
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
    @com.libraryms.audit.Audited(entityType = "AUTHOR", operation = "UPDATE")
    public AuthorResponse updateAuthor(Long id, UpdateAuthorRequest request) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));
        authorMapper.updateEntity(request, author);
        return authorMapper.toResponse(author);
    }

    @Transactional
    @com.libraryms.audit.Audited(entityType = "AUTHOR", operation = "DELETE")
    public void deleteAuthor(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));
        // Deleting an author removes join rows via database foreign key ON DELETE CASCADE, never books or copies
        authorRepository.delete(author);
    }
}
