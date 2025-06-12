package com.nextdoor.nextdoor.domain.rentalreservation.domain.repository;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.AiComparisonResult;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.RentalReservation;

import java.util.Optional;

public interface RentalReservationRepository{

    Optional<RentalReservation> findById(Long id);
    Optional<AiComparisonResult> findRentalWithImagesByRentalId(Long rentalId);
    RentalReservation save(RentalReservation rentalReservation);
    void delete(RentalReservation rentalReservation);
}
