package com.libraryms.audit;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.libraryms.audit.dto.AuditLogResponse;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * Standard audit log: runs in current transaction so entity write and audit row commit or roll back together.
     * Guaranteed never to throw an unhandled exception that disrupts the caller.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void logEvent(Long userId, String username, String entityType, Long entityId,
                         String operation, String details) {
        try {
            AuditLog auditLog = new AuditLog(
                    userId,
                    username != null ? username : "system",
                    entityType,
                    entityId,
                    operation,
                    Instant.now(),
                    details
            );
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Failed to persist transactional audit log: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Autonomous audit log (e.g. login failure or unauthenticated events):
     * runs in a NEW transaction so the record survives even if the calling request rolls back or fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAutonomousEvent(Long userId, String username, String entityType, Long entityId,
                                   String operation, String details) {
        try {
            AuditLog auditLog = new AuditLog(
                    userId,
                    username != null ? username : "system",
                    entityType,
                    entityId,
                    operation,
                    Instant.now(),
                    details
            );
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Failed to persist autonomous audit log: {}", ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listAuditLogs(
            String entityType,
            Long entityId,
            String username,
            String operation,
            Instant from,
            Instant to,
            Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();

        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("entityType")), entityType.trim().toLowerCase()));
        }
        if (entityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityId"), entityId));
        }
        if (username != null && !username.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("username")), "%" + username.trim().toLowerCase() + "%"));
        }
        if (operation != null && !operation.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("operation")), operation.trim().toUpperCase()));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        }

        return auditLogRepository.findAll(spec, pageable).map(auditLogMapper::toResponse);
    }
}
