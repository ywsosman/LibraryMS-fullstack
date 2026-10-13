package com.libraryms.repository;

import com.libraryms.model.Book;
import com.libraryms.model.BookCopy;
import com.libraryms.enums.CopyStatus;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long>, JpaSpecificationExecutor<BookCopy> {

    @EntityGraph(attributePaths = "book")
    Optional<BookCopy> findWithBookById(Long id);

    boolean existsByBarcode(String barcode);

    boolean existsByBookId(Long bookId);

    long countByBookId(Long bookId);

    long countByBookIdAndStatus(Long bookId, CopyStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT c.book.id, count(c), sum(CASE WHEN c.status = com.libraryms.enums.CopyStatus.AVAILABLE THEN 1L ELSE 0L END) " +
           "FROM BookCopy c WHERE c.book.id IN :bookIds GROUP BY c.book.id")
    java.util.List<Object[]> countCopiesByBookIds(@org.springframework.data.repository.query.Param("bookIds") java.util.Collection<Long> bookIds);
}
