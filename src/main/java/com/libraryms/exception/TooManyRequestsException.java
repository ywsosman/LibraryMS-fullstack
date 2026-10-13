package com.libraryms.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String message) {
        super(ErrorCode.TOO_MANY_ATTEMPTS, HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
