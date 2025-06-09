package com.nextdoor.nextdoor.domain.rentalreservation.exception;

public abstract class BaseCustomException extends RuntimeException {

    public BaseCustomException() {
        super();
    }

    public BaseCustomException(String message) {
        super(message);
    }

    public BaseCustomException(String message, Throwable cause) {
        super(message, cause);
    }

    public BaseCustomException(Throwable cause) {
        super(cause);
    }

    abstract public String getErrorCode();
}