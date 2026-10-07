package com.libraryms.common.error;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<String> details,
        Instant timestamp,
        String path
) {
    public ApiErrorResponse {
        if (details == null) {
            details = Collections.emptyList();
        } else {
            details = List.copyOf(details);
        }
    }

    public static ApiErrorResponse of(ErrorCode code, String message, List<String> details, String path) {
        return new ApiErrorResponse(code.name(), message, details, Instant.now(), path);
    }

    public static ApiErrorResponse of(ErrorCode code, String message, String path) {
        return new ApiErrorResponse(code.name(), message, Collections.emptyList(), Instant.now(), path);
    }
}
