package com.nextdoor.nextdoor.domain.rentalreservation.exception;

import lombok.Getter;

@Getter
public class InvalidRenterIdException extends BaseCustomException {

    private final String errorCode = "INVALID_RENTER_ID";

    public InvalidRenterIdException() {
        super("유효하지 않은 대여자 ID입니다.");
    }

    public InvalidRenterIdException(String message) {
        super(message);
    }

    public InvalidRenterIdException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidRenterIdException(Throwable cause) {
        super(cause);
    }
}