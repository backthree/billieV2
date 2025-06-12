package com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.persistence.jpa;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.RentalReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalReservationJpaRepository extends JpaRepository<RentalReservation, Long> {
}
