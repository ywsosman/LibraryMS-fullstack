package com.libraryms.author.service;

import com.libraryms.author.entity.Author;
import com.libraryms.author.mapper.AuthorMapper;
import com.libraryms.author.repository.AuthorRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

import com.libraryms.author.dto.AuthorResponse;
import com.libraryms.author.dto.CreateAuthorRequest;
import com.libraryms.author.dto.UpdateAuthorRequest;
import com.libraryms.common.error.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private AuthorMapper authorMapper;

    @InjectMocks
    private AuthorService authorService;

    @Test
    @DisplayName("createAuthor should map, save, and return response")
    void createAuthor_success() {
        CreateAuthorRequest request = new CreateAuthorRequest("George Orwell", "Bio", LocalDate.of(1903, 6, 25));
        Author author = new Author("George Orwell", "Bio", LocalDate.of(1903, 6, 25));
        Author saved = new Author("George Orwell", "Bio", LocalDate.of(1903, 6, 25));
        AuthorResponse response = new AuthorResponse(1L, "George Orwell", "Bio", LocalDate.of(1903, 6, 25), null, null);

        when(authorMapper.toEntity(request)).thenReturn(author);
        when(authorRepository.save(author)).thenReturn(saved);
        when(authorMapper.toResponse(saved)).thenReturn(response);

        AuthorResponse result = authorService.createAuthor(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("George Orwell");
    }

    @Test
    @DisplayName("getAuthorById should return response if author exists")
    void getAuthorById_found() {
        Author author = new Author("George Orwell", "Bio", null);
        AuthorResponse response = new AuthorResponse(1L, "George Orwell", "Bio", null, null, null);

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorMapper.toResponse(author)).thenReturn(response);

        AuthorResponse result = authorService.getAuthorById(1L);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("George Orwell");
    }

    @Test
    @DisplayName("getAuthorById should throw ResourceNotFoundException if author does not exist")
    void getAuthorById_notFound() {
        when(authorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getAuthorById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author");
    }

    @Test
    @DisplayName("listAuthors should return paged responses")
    void listAuthors_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Author author = new Author("George Orwell", "Bio", null);
        AuthorResponse response = new AuthorResponse(1L, "George Orwell", "Bio", null, null, null);
        Page<Author> pagedAuthors = new PageImpl<>(List.of(author), pageable, 1);

        when(authorRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(pagedAuthors);
        when(authorMapper.toResponse(author)).thenReturn(response);

        Page<AuthorResponse> result = authorService.listAuthors("Orwell", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("George Orwell");
    }

    @Test
    @DisplayName("updateAuthor should update fields and return response")
    void updateAuthor_success() {
        UpdateAuthorRequest request = new UpdateAuthorRequest("Eric Blair", "New Bio", null);
        Author author = new Author("George Orwell", "Old Bio", null);
        AuthorResponse response = new AuthorResponse(1L, "Eric Blair", "New Bio", null, null, null);

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorMapper.toResponse(author)).thenReturn(response);

        AuthorResponse result = authorService.updateAuthor(1L, request);

        verify(authorMapper).updateEntity(request, author);
        assertThat(result.name()).isEqualTo("Eric Blair");
    }

    @Test
    @DisplayName("updateAuthor should throw ResourceNotFoundException if not found")
    void updateAuthor_notFound() {
        UpdateAuthorRequest request = new UpdateAuthorRequest("Eric Blair", null, null);
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.updateAuthor(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteAuthor should delete author if found")
    void deleteAuthor_success() {
        Author author = new Author("George Orwell", "Bio", null);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        authorService.deleteAuthor(1L);

        verify(authorRepository).delete(author);
    }

    @Test
    @DisplayName("deleteAuthor should throw ResourceNotFoundException if not found")
    void deleteAuthor_notFound() {
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.deleteAuthor(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
