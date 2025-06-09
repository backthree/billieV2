package com.nextdoor.nextdoor.domain.rentalreservation.port;

import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.ReservationMemberQueryDto;

import java.util.Optional;

public interface ReservationMemberQueryPort {

    Optional<ReservationMemberQueryDto> findById(Long id);
}