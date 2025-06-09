package com.nextdoor.nextdoor.domain.rentalreservation.exception;

import lombok.Getter;

@Getter
public class NoSuchRentalException extends BaseCustomException {

    private final String errorCode = "NO_SUCH_RENTAL";

    public NoSuchRentalException() {
        super("대여 정보가 존재하지 않습니다.");
    }

    public NoSuchRentalException(String message) {
        super(message);
    }

    public NoSuchRentalException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchRentalException(Throwable cause) {
        super(cause);
    }
}