package com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.repository;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.AiComparisonResult;

import java.util.Optional;

public interface RentalReservationCustomRepository {

    Optional<AiComparisonResult> findRentalWithImagesByRentalId(Long rentalId);
}