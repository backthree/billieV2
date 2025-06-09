package com.nextdoor.nextdoor.domain.rentalreservation.exception;

import lombok.Getter;

@Getter
public class InvalidAmountException extends BaseCustomException {

    private final String errorCode = "INVALID_AMOUNT";

    public InvalidAmountException() {
        super("유효하지 않은 금액입니다.");
    }

    public InvalidAmountException(String message) {
        super(message);
    }

    public InvalidAmountException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAmountException(Throwable cause) {
        super(cause);
    }
}