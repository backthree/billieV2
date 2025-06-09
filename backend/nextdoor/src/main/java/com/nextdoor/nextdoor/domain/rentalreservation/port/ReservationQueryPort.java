package com.nextdoor.nextdoor.domain.rentalreservation.port;

import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.ReservationDto;

import java.util.Optional;

public interface ReservationQueryPort {

    Optional<ReservationDto> getReservationByRentalId(Long rentalId);
}