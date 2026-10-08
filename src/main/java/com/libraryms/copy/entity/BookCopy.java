package com.libraryms.copy.entity;

import com.libraryms.book.entity.Book;
import com.libraryms.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A physical copy of a book. Its {@code @Version} is what makes borrowing concurrency-safe:
 * two transactions flipping the same copy to ON_LOAN cannot both commit.
 */
@Entity
@Table(name = "book_copies")
@Getter
@Setter
@NoArgsConstructor
public class BookCopy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "barcode", nullable = false, unique = true, length = 64)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CopyStatus status = CopyStatus.AVAILABLE;

    public BookCopy(Book book, String barcode) {
        this.book = book;
        this.barcode = barcode;
    }

    @Override
    public String toString() {
        return "BookCopy{id=" + getId() + ", barcode='" + barcode + "', status=" + status + "}";
    }
}
