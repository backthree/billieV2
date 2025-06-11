package com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.request;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.entity.RentalReservationStatus;
import lombok.Getter;

@Getter
public class ReservationStatusUpdateRequestDto {

    private RentalReservationStatus status;
}