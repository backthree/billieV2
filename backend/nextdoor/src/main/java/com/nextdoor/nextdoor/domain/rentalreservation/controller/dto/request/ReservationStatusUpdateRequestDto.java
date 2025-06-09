package com.nextdoor.nextdoor.domain.rentalreservation.controller.dto.request;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservationStatus;
import lombok.Getter;

@Getter
public class ReservationStatusUpdateRequestDto {

    private RentalReservationStatus status;
}