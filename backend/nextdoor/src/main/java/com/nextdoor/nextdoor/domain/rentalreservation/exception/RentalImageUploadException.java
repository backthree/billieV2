package com.nextdoor.nextdoor.domain.rentalreservation.exception;

import lombok.Getter;

@Getter
public class RentalImageUploadException extends BaseCustomException {

    private final String errorCode = "RENTAL_IMAGE_UPLOAD_FAILED";

    public RentalImageUploadException() {
        super("대여 이미지 업로드가 실패했습니다.");
    }

    public RentalImageUploadException(String message) {
        super(message);
    }

    public RentalImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public RentalImageUploadException(Throwable cause) {
        super(cause);
    }
}
