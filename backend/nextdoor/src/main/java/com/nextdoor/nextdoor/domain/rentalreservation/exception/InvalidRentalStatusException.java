package com.nextdoor.nextdoor.domain.rentalreservation.exception;

import lombok.Getter;

@Getter
public class InvalidRentalStatusException extends BaseCustomException {

    private final String errorCode = "INVALID_RENTAL_STATUS";

    public InvalidRentalStatusException() {
        super("잘못된 대여 상태입니다.");
    }

    public InvalidRentalStatusException(String message) {
        super(message);
    }

    public InvalidRentalStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidRentalStatusException(Throwable cause) {
        super(cause);
    }
}
