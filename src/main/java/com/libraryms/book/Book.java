package com.libraryms.book;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.libraryms.author.Author;
import com.libraryms.common.persistence.BaseEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bibliographic record. Copies reference the book (unidirectional) so that a book never
 * serializes or cascades into its copies or loans.
 */
@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /** Normalized ISBN-13 (digits only). */
    @Column(name = "isbn", nullable = false, unique = true, length = 13)
    private String isbn;

    @Column(name = "published_date", nullable = false)
    private LocalDate publishedDate;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** No cascade: deleting a book removes only join rows (DB-side), never authors. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    @Setter(AccessLevel.NONE)
    private Set<Author> authors = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "book_genres", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "genre", nullable = false, length = 50)
    @Setter(AccessLevel.NONE)
    private Set<String> genres = new HashSet<>();

    public Book(String title, String isbn, LocalDate publishedDate) {
        this.title = title;
        this.isbn = isbn;
        this.publishedDate = publishedDate;
    }

    public void replaceAuthors(Set<Author> newAuthors) {
        authors.clear();
        authors.addAll(newAuthors);
    }

    public void replaceGenres(Set<String> newGenres) {
        genres.clear();
        genres.addAll(newGenres);
    }

    @Override
    public String toString() {
        return "Book{id=" + getId() + ", isbn='" + isbn + "'}";
    }
}
