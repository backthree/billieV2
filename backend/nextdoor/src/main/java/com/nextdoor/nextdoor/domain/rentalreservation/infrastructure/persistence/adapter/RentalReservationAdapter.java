package com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.persistence.adapter;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.AiComparisonResult;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.RentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.repository.RentalReservationRepository;
import com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.persistence.custom.RentalReservationCustomRepository;
import com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.persistence.jpa.RentalReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RentalReservationAdapter implements RentalReservationRepository {

    private final RentalReservationJpaRepository jpaRepository;
    private final RentalReservationCustomRepository customRepository;

    @Override
    public Optional<RentalReservation> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<AiComparisonResult> findRentalWithImagesByRentalId(Long rentalId) {
        return customRepository.findRentalWithImagesByRentalId(rentalId);
    }

    @Override
    public RentalReservation save(RentalReservation rentalReservation) {
        return jpaRepository.save(rentalReservation);
    }

    @Override
    public void delete(RentalReservation rentalReservation) {
        jpaRepository.delete(rentalReservation);
    }
}
