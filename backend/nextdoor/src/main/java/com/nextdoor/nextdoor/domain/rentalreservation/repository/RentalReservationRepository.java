package com.nextdoor.nextdoor.domain.rentalreservation.repository;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.RentalReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RentalReservationRepository extends JpaRepository<RentalReservation, Long>, RentalReservationCustomRepository {

    Optional<RentalReservation> findById(Long id);
}
