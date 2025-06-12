package com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.persistence.jpa;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.AiImageComparisonPair;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiImageComparisonPairJpaRepository  extends JpaRepository<AiImageComparisonPair, Long> {

    void deleteByRentalId(Long rentalId);
}
