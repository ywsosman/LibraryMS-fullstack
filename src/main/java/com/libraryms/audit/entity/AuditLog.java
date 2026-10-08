package com.libraryms.audit.entity;

import com.libraryms.user.entity.User;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable audit record. {@code userId} is a plain nullable column (FK ON DELETE SET NULL in SQL),
 * not a JPA relation, so audit rows never block or cascade with user deletion.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", updatable = false)
    private Long userId;

    @Column(name = "username", nullable = false, length = 100, updatable = false)
    private String username;

    @Column(name = "entity_type", nullable = false, length = 50, updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private Long entityId;

    @Column(name = "operation", nullable = false, length = 32, updatable = false)
    private String operation;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "details", columnDefinition = "text", updatable = false)
    private String details;

    public AuditLog(Long userId, String username, String entityType, Long entityId,
                    String operation, Instant occurredAt, String details) {
        this.userId = userId;
        this.username = username;
        this.entityType = entityType;
        this.entityId = entityId;
        this.operation = operation;
        this.occurredAt = occurredAt;
        this.details = details;
    }
}
