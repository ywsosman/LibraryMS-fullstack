package com.libraryms.common.error;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String message) {
        super(ErrorCode.TOO_MANY_ATTEMPTS, HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
