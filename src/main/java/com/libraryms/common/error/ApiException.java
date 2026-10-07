package com.libraryms.common.error;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Base domain exception with an immutable error code, HTTP status, and optional details list.
 * Contains no public mutable fields.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;
    private final List<String> details;

    public ApiException(ErrorCode errorCode, HttpStatus status, String message) {
        this(errorCode, status, message, Collections.emptyList());
    }

    public ApiException(ErrorCode errorCode, HttpStatus status, String message, List<String> details) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.details = details == null ? Collections.emptyList() : List.copyOf(details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getDetails() {
        return details;
    }
}
