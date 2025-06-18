package com.nextdoor.nextdoor.domain.rentalreservation.domain.service;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.RentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.RentalReservationStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 대여 관련 순수 도메인 로직을 처리하는 도메인 서비스
 * 엔티티나 값 객체에 자연스럽게 속하지 않는 비즈니스 규칙을 구현
 */
@Service
public class RentalCompletionProcessService {

    public void processAfterImageRegistration(RentalReservation rentalReservation, BigDecimal depositAmount) {
        if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            rentalReservation.changeStatus(RentalReservationStatus.BEFORE_AND_AFTER_COMPARED);
        } else {
            rentalReservation.changeStatus(RentalReservationStatus.RENTAL_COMPLETED);
        }
    }
}