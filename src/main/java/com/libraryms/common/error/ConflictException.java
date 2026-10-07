package com.libraryms.common.error;

import java.util.List;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, HttpStatus.CONFLICT, message);
    }

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.CONFLICT, message);
    }

    public ConflictException(String message, List<String> details) {
        super(ErrorCode.CONFLICT, HttpStatus.CONFLICT, message, details);
    }
}
