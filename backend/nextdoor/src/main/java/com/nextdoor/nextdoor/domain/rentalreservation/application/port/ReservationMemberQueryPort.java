package com.nextdoor.nextdoor.domain.rentalreservation.application.port;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.ReservationMemberQueryDto;

import java.util.Optional;

public interface ReservationMemberQueryPort {

    Optional<ReservationMemberQueryDto> findById(Long id);
}