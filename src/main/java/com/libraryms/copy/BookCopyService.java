package com.libraryms.copy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.book.Book;
import com.libraryms.book.BookRepository;
import com.libraryms.common.error.ConflictException;
import com.libraryms.common.error.ResourceNotFoundException;
import com.libraryms.copy.dto.CopyResponse;
import com.libraryms.copy.dto.CreateCopyRequest;
import com.libraryms.copy.dto.UpdateCopyStatusRequest;
import com.libraryms.loan.LoanRepository;

@Service
public class BookCopyService {

    private final BookCopyRepository copyRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final BookCopyMapper copyMapper;

    public BookCopyService(BookCopyRepository copyRepository,
                           BookRepository bookRepository,
                           LoanRepository loanRepository,
                           BookCopyMapper copyMapper) {
        this.copyRepository = copyRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.copyMapper = copyMapper;
    }

    @Transactional
    public CopyResponse createCopy(CreateCopyRequest request) {
        String barcode = request.barcode().trim();
        if (copyRepository.existsByBarcode(barcode)) {
            throw new ConflictException("Book copy with barcode " + barcode + " already exists");
        }

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", request.bookId()));

        BookCopy copy = new BookCopy(book, barcode);
        copy.setStatus(CopyStatus.AVAILABLE);

        BookCopy saved = copyRepository.save(copy);
        return copyMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CopyResponse getCopyById(Long id) {
        BookCopy copy = copyRepository.findWithBookById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookCopy", id));
        return copyMapper.toResponse(copy);
    }

    @Transactional(readOnly = true)
    public Page<CopyResponse> listCopies(Long bookId, String barcode, CopyStatus status, Pageable pageable) {
        Specification<BookCopy> spec = Specification.where(null);

        if (bookId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("book").get("id"), bookId));
        }
        if (barcode != null && !barcode.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("barcode")), "%" + barcode.trim().toLowerCase() + "%"));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return copyRepository.findAll(spec, pageable).map(copyMapper::toResponse);
    }

    @Transactional
    public CopyResponse updateCopyStatus(Long id, UpdateCopyStatusRequest request) {
        BookCopy copy = copyRepository.findWithBookById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookCopy", id));

        if (copy.getStatus() == CopyStatus.ON_LOAN && request.status() == CopyStatus.AVAILABLE) {
            throw new ConflictException("Cannot manually mark an active ON_LOAN copy as AVAILABLE. Use the loan return endpoint.");
        }

        copy.setStatus(request.status());
        return copyMapper.toResponse(copy);
    }

    @Transactional
    public void deleteCopy(Long id) {
        BookCopy copy = copyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookCopy", id));

        if (loanRepository.existsByCopyId(id)) {
            throw new ConflictException("Cannot delete copy with loan history. Mark its status as LOST instead.");
        }

        copyRepository.delete(copy);
    }
}
