package com.libraryms.repository;

import com.libraryms.model.Book;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    @EntityGraph(attributePaths = {"authors", "genres"})
    Optional<Book> findDetailedById(Long id);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Book b JOIN b.authors a WHERE a.id = :authorId AND SIZE(b.authors) = 1")
    java.util.List<Book> findBooksWhereSoleAuthor(@org.springframework.data.repository.query.Param("authorId") Long authorId);
}
