package com.nextdoor.nextdoor.domain.rentalreservation.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ErrorResponse {

    private LocalDateTime timestamp;
    private String errorCode;
    private String message;
    private String path;

    public ErrorResponse(String errorCode, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
    }
}