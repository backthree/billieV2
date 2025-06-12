package com.nextdoor.nextdoor.domain.rentalreservation.application.port;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.ReservationDto;

import java.util.Optional;

public interface ReservationQueryPort {

    Optional<ReservationDto> getReservationByRentalId(Long rentalId);
}