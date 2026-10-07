package com.libraryms.copy;

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

import com.libraryms.book.Book;
import com.libraryms.book.BookRepository;
import com.libraryms.common.dto.summary.BookSummary;
import com.libraryms.common.error.ConflictException;
import com.libraryms.common.error.ResourceNotFoundException;
import com.libraryms.copy.dto.CopyResponse;
import com.libraryms.copy.dto.CreateCopyRequest;
import com.libraryms.copy.dto.UpdateCopyStatusRequest;
import com.libraryms.loan.LoanRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookCopyServiceTest {

    @Mock
    private BookCopyRepository copyRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookCopyMapper copyMapper;

    @InjectMocks
    private BookCopyService copyService;

    @Test
    @DisplayName("createCopy should save copy and return response")
    void createCopy_success() {
        CreateCopyRequest request = new CreateCopyRequest(1L, "BAR-1001");
        Book book = new Book("Title", "9780452284241", null);
        BookCopy copy = new BookCopy(book, "BAR-1001");
        CopyResponse response = new CopyResponse(1L, new BookSummary(1L, "Title", "9780452284241"), "BAR-1001", CopyStatus.AVAILABLE, null, null);

        when(copyRepository.existsByBarcode("BAR-1001")).thenReturn(false);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(copyRepository.save(any(BookCopy.class))).thenReturn(copy);
        when(copyMapper.toResponse(copy)).thenReturn(response);

        CopyResponse result = copyService.createCopy(request);

        assertThat(result.barcode()).isEqualTo("BAR-1001");
        assertThat(result.status()).isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("createCopy should throw ConflictException if barcode exists")
    void createCopy_duplicateBarcode_throwsConflict() {
        CreateCopyRequest request = new CreateCopyRequest(1L, "BAR-1001");
        when(copyRepository.existsByBarcode("BAR-1001")).thenReturn(true);

        assertThatThrownBy(() -> copyService.createCopy(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("createCopy should throw ResourceNotFoundException if book not found")
    void createCopy_bookNotFound_throwsNotFound() {
        CreateCopyRequest request = new CreateCopyRequest(999L, "BAR-1001");
        when(copyRepository.existsByBarcode("BAR-1001")).thenReturn(false);
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> copyService.createCopy(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getCopyById should return response if copy exists")
    void getCopyById_found() {
        BookCopy copy = new BookCopy(new Book("Title", "9780452284241", null), "BAR-1001");
        CopyResponse response = new CopyResponse(1L, new BookSummary(1L, "Title", "9780452284241"), "BAR-1001", CopyStatus.AVAILABLE, null, null);

        when(copyRepository.findWithBookById(1L)).thenReturn(Optional.of(copy));
        when(copyMapper.toResponse(copy)).thenReturn(response);

        CopyResponse result = copyService.getCopyById(1L);

        assertThat(result.barcode()).isEqualTo("BAR-1001");
    }

    @Test
    @DisplayName("getCopyById should throw ResourceNotFoundException if copy does not exist")
    void getCopyById_notFound() {
        when(copyRepository.findWithBookById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> copyService.getCopyById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listCopies should return paged copies")
    void listCopies_success() {
        Pageable pageable = PageRequest.of(0, 10);
        BookCopy copy = new BookCopy(new Book("Title", "9780452284241", null), "BAR-1001");
        CopyResponse response = new CopyResponse(1L, null, "BAR-1001", CopyStatus.AVAILABLE, null, null);
        Page<BookCopy> page = new PageImpl<>(List.of(copy), pageable, 1);

        when(copyRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(copyMapper.toResponse(copy)).thenReturn(response);

        Page<CopyResponse> result = copyService.listCopies(1L, "BAR", CopyStatus.AVAILABLE, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("updateCopyStatus should update status")
    void updateCopyStatus_success() {
        BookCopy copy = new BookCopy(new Book("Title", "9780452284241", null), "BAR-1001");
        copy.setStatus(CopyStatus.AVAILABLE);
        UpdateCopyStatusRequest request = new UpdateCopyStatusRequest(CopyStatus.LOST);
        CopyResponse response = new CopyResponse(1L, null, "BAR-1001", CopyStatus.LOST, null, null);

        when(copyRepository.findWithBookById(1L)).thenReturn(Optional.of(copy));
        when(copyMapper.toResponse(copy)).thenReturn(response);

        CopyResponse result = copyService.updateCopyStatus(1L, request);

        assertThat(result.status()).isEqualTo(CopyStatus.LOST);
        assertThat(copy.getStatus()).isEqualTo(CopyStatus.LOST);
    }

    @Test
    @DisplayName("updateCopyStatus should reject marking ON_LOAN copy directly as AVAILABLE")
    void updateCopyStatus_onLoanToAvailable_throwsConflict() {
        BookCopy copy = new BookCopy(new Book("Title", "9780452284241", null), "BAR-1001");
        copy.setStatus(CopyStatus.ON_LOAN);
        UpdateCopyStatusRequest request = new UpdateCopyStatusRequest(CopyStatus.AVAILABLE);

        when(copyRepository.findWithBookById(1L)).thenReturn(Optional.of(copy));

        assertThatThrownBy(() -> copyService.updateCopyStatus(1L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot manually mark an active ON_LOAN copy as AVAILABLE");
    }

    @Test
    @DisplayName("deleteCopy should delete copy if no loan history")
    void deleteCopy_success() {
        BookCopy copy = new BookCopy(new Book("Title", "9780452284241", null), "BAR-1001");
        when(copyRepository.findById(1L)).thenReturn(Optional.of(copy));
        when(loanRepository.existsByCopyId(1L)).thenReturn(false);

        copyService.deleteCopy(1L);

        verify(copyRepository).delete(copy);
    }

    @Test
    @DisplayName("deleteCopy should throw ConflictException if copy has loan history")
    void deleteCopy_withLoanHistory_throwsConflict() {
        BookCopy copy = new BookCopy(new Book("Title", "9780452284241", null), "BAR-1001");
        when(copyRepository.findById(1L)).thenReturn(Optional.of(copy));
        when(loanRepository.existsByCopyId(1L)).thenReturn(true);

        assertThatThrownBy(() -> copyService.deleteCopy(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("loan history");
    }

    @Test
    @DisplayName("deleteCopy should throw ResourceNotFoundException if copy not found")
    void deleteCopy_notFound() {
        when(copyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> copyService.deleteCopy(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
