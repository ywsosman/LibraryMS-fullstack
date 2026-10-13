package com.libraryms.dto;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long userId,
        String username,
        String entityType,
        Long entityId,
        String operation,
        Instant occurredAt,
        String details
) {}
