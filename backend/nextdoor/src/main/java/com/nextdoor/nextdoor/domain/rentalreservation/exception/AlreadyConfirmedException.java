package com.nextdoor.nextdoor.domain.rentalreservation.exception;

import lombok.Getter;

@Getter
public class AlreadyConfirmedException extends BaseCustomException {

    private final String errorCode = "RESERVATION_ALREADY_CONFIRMED";

    public AlreadyConfirmedException() {
        super();
    }

    public AlreadyConfirmedException(String message) {
        super(message);
    }

    public AlreadyConfirmedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyConfirmedException(Throwable cause) {
        super(cause);
    }
}