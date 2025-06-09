package com.nextdoor.nextdoor.domain.rentalreservation.repository;

import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.AiComparisonResult;

import java.util.Optional;

public interface RentalReservationCustomRepository {

    Optional<AiComparisonResult> findRentalWithImagesByRentalId(Long rentalId);
}