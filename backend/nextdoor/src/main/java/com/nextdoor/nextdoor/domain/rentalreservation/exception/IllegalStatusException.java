package com.nextdoor.nextdoor.domain.rentalreservation.exception;

import lombok.Getter;

@Getter
public class IllegalStatusException extends BaseCustomException {

    private final String errorCode = "ILLEGAL_STATUS";

    public IllegalStatusException() {
        super();
    }

    public IllegalStatusException(String message) {
        super(message);
    }

    public IllegalStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalStatusException(Throwable cause) {
        super(cause);
    }
}