package com.libraryms.copy;

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
}
