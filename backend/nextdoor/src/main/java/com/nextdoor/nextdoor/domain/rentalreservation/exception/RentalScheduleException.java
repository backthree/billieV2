package com.nextdoor.nextdoor.domain.rentalreservation.exception;

import lombok.Getter;

@Getter
public class RentalScheduleException extends BaseCustomException {

    private final String errorCode = "RENTAL_SCHEDULE_FAILED";

    public RentalScheduleException() {
        super("대여 종료 스케줄 생성에 실패했습니다.");
    }

    public RentalScheduleException(String message) {
        super(message);
    }

    public RentalScheduleException(String message, Throwable cause) {
        super(message, cause);
    }

    public RentalScheduleException(Throwable cause) {
        super(cause);
    }
}
