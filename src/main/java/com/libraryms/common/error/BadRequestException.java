package com.libraryms.common.error;

import java.util.List;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
    }

    public BadRequestException(String message, List<String> details) {
        super(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message, details);
    }
}
