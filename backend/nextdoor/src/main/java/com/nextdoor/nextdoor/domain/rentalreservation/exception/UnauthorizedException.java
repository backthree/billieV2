package com.nextdoor.nextdoor.domain.rentalreservation.exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends BaseCustomException {

    private final String errorCode = "UNAUTHORIZED";

    public UnauthorizedException() {
        super();
    }

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedException(Throwable cause) {
        super(cause);
    }
}