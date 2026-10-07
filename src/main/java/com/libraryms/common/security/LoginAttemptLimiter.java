package com.libraryms.common.security;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory thread-safe rate limiter and lockout manager for login attempts.
 * Tracks failed attempts per username key and enforces a temporary lockout after N failures.
 * Uses an injectable Clock for deterministic unit and integration testing.
 */
@Component
public class LoginAttemptLimiter {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_SECONDS = 900; // 15 minutes

    private final Clock clock;
    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    public LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isLocked(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.trim().toLowerCase();
        AttemptRecord record = attempts.get(normalized);
        if (record == null) {
            return false;
        }
        Instant now = clock.instant();
        if (record.isExpired(now)) {
            attempts.remove(normalized);
            return false;
        }
        return record.failedCount >= MAX_FAILED_ATTEMPTS;
    }

    public void recordFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        String normalized = key.trim().toLowerCase();
        Instant now = clock.instant();
        attempts.compute(normalized, (k, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return new AttemptRecord(1, now.plusSeconds(LOCKOUT_DURATION_SECONDS));
            }
            return new AttemptRecord(existing.failedCount + 1, now.plusSeconds(LOCKOUT_DURATION_SECONDS));
        });
    }

    public void recordSuccess(String key) {
        if (key != null) {
            attempts.remove(key.trim().toLowerCase());
        }
    }

    public void resetAll() {
        attempts.clear();
    }

    private record AttemptRecord(int failedCount, Instant lockoutUntil) {
        boolean isExpired(Instant now) {
            return now.isAfter(lockoutUntil);
        }
    }
}
