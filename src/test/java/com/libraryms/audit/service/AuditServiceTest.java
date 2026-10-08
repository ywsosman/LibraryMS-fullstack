package com.libraryms.audit.service;

import com.libraryms.audit.mapper.AuditLogMapper;
import com.libraryms.audit.repository.AuditLogRepository;
import com.libraryms.audit.entity.AuditLog;
import com.libraryms.user.entity.User;
import com.libraryms.book.entity.Book;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.libraryms.audit.dto.AuditLogResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("logEvent saves audit log with actor details")
    void logEvent_success() {
        auditService.logEvent(1L, "admin", "BOOK", 10L, "CREATE", "Created book 10");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertThat(captured.getUserId()).isEqualTo(1L);
        assertThat(captured.getUsername()).isEqualTo("admin");
        assertThat(captured.getEntityType()).isEqualTo("BOOK");
        assertThat(captured.getEntityId()).isEqualTo(10L);
        assertThat(captured.getOperation()).isEqualTo("CREATE");
        assertThat(captured.getDetails()).isEqualTo("Created book 10");
        assertThat(captured.getOccurredAt()).isNotNull();
    }

    @Test
    @DisplayName("logEvent defaults username to 'system' when null")
    void logEvent_nullUsernameDefaultsToSystem() {
        auditService.logEvent(null, null, "BOOK", 10L, "CREATE", "Created book 10");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertThat(captured.getUsername()).isEqualTo("system");
    }

    @Test
    @DisplayName("logEvent catches repository exceptions without propagating")
    void logEvent_repositoryThrows_catchesAndLogs() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        assertThatCode(() -> auditService.logEvent(1L, "admin", "BOOK", 10L, "CREATE", "details"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logAutonomousEvent saves audit log with actor details")
    void logAutonomousEvent_success() {
        auditService.logAutonomousEvent(null, "baduser", "USER", null, "LOGIN_FAILED", "Bad password");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog captured = captor.getValue();
        assertThat(captured.getUsername()).isEqualTo("baduser");
        assertThat(captured.getOperation()).isEqualTo("LOGIN_FAILED");
    }

    @Test
    @DisplayName("logAutonomousEvent catches repository exceptions without propagating")
    void logAutonomousEvent_repositoryThrows_catchesAndLogs() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThatCode(() -> auditService.logAutonomousEvent(null, "user", "USER", null, "LOGIN", "details"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("listAuditLogs returns paged audit log responses")
    void listAuditLogs_success() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = new AuditLog(1L, "admin", "BOOK", 10L, "CREATE", Instant.now(), "details");
        Page<AuditLog> page = new PageImpl<>(List.of(log), pageable, 1);
        AuditLogResponse response = new AuditLogResponse(1L, 1L, "admin", "BOOK", 10L, "CREATE", Instant.now(), "details");

        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(auditLogMapper.toResponse(log)).thenReturn(response);

        Page<AuditLogResponse> result = auditService.listAuditLogs(
                "BOOK", 10L, "admin", "CREATE", Instant.now().minusSeconds(3600), Instant.now(), pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).username()).isEqualTo("admin");
    }
}
